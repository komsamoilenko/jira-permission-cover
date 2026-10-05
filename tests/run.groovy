// Behaviour tests for the real planner; Jira services below are offline boundary doubles.
def root = new File(args ? args[0] : '.').canonicalFile
def source = new File(root, 'src/PermissionCover.groovy')
assert source.isFile(): 'Read-only planner has not been implemented yet'
def loader = new GroovyClassLoader(getClass().classLoader)
loader.parseClass(source)
def planner = loader.loadClass('PermissionCover')
int passed = 0
def test = { String name, Closure body ->
    try { body(); passed++; println "PASS ${name}" }
    catch (Throwable failure) { System.err.println("FAIL ${name}"); throw failure }
}
def set = { values -> values as Set }
def groups = [alpha: set(['u1', 'u2', 'u3']), beta: set(['u3', 'u4']),
              gamma: set(['u4', 'u5']), delta: set(['u6'])]
def holders = set(['u1', 'u2', 'u3', 'u4', 'u5', 'u6'])

test('picks by NEW coverage, keeping people missed by the two biggest groups') {
    def result = planner.analyse(holders, groups)
    assert result.status == 'EXACT_SNAPSHOT'
    assert result.selectedGroups == ['alpha', 'gamma', 'delta']
    assert result.unselectedGroups == ['beta']
    assert result.holderCount == 6 && result.lostCount == 0 && result.gainedCount == 0
}
test('ties are deterministic regardless of map insertion order') {
    assert planner.analyse(set(['u']), [zulu: set(['u']), alpha: set(['u'])]).selectedGroups == ['alpha']
}
test('an uncovered effective holder blocks the analysis') {
    def result = planner.analyse(set(['u', 'missing']), [alpha: set(['u'])])
    assert result.status == 'UNSUPPORTED_COVERAGE'
    assert !result.selectedGroups && !result.unselectedGroups
    assert result.lostCount == 1
}
test('members without effective permission block the analysis') {
    def result = planner.analyse(set(['u']), [alpha: set(['u', 'extra'])])
    assert result.status == 'UNSUPPORTED_COVERAGE' && result.gainedCount == 1
    assert !result.unselectedGroups
}
test('an empty scope never recommends all grants for removal') {
    def result = planner.analyse([] as Set, [alpha: [] as Set])
    assert result.status == 'EMPTY_SCOPE' && !result.unselectedGroups
}
test('empty effective holders do not hide a nonempty granted-group union') {
    def result = planner.analyse([] as Set, [alpha:['u'] as Set])
    assert result.status == 'UNSUPPORTED_COVERAGE' && result.gainedCount == 1
    assert !result.selectedGroups && !result.unselectedGroups
}
test('a missing candidate list does not masquerade as an exact result') {
    assert planner.analyse(set(['u']), [:]).status == 'UNSUPPORTED_COVERAGE'
}
test('analysis does not mutate caller-owned data') {
    def before = groups.collectEntries { k, v -> [k, new HashSet(v)] }
    planner.analyse(holders, groups)
    assert groups == before && holders == set(['u1', 'u2', 'u3', 'u4', 'u5', 'u6'])
}
test('greedy is not misrepresented as a minimum-cardinality solution') {
    def fixture = [alpha:set([1,2,3,4]), beta:set([1,2,5]), gamma:set([3,4,6])]
    def result = planner.analyse(set([1,2,3,4,5,6]), fixture)
    assert result.selectedGroups == ['alpha', 'beta', 'gamma']
    // beta + gamma alone suffice, although greedy selects three.
    assert (fixture.beta + fixture.gamma) == set([1,2,3,4,5,6])
}
test('null input is rejected rather than treated as empty') {
    try { planner.analyse(null, [:]); assert false: 'Expected validation failure' }
    catch (IllegalArgumentException expected) { }
}
test('adversarial high-overlap input stops at a computation budget without a partial plan') {
    def common = (0..<50) as Set
    def fixture = (0..<40).collectEntries { i ->
        [(String.format('group-%02d', i)): (common + [100 + i]) as Set]
    }
    def result = planner.analyse(fixture.values().flatten() as Set, fixture, 4000L)
    assert result.status == 'BLOCKED' && result.reason == 'WORK_BUDGET'
    assert !result.selectedGroups && !result.unselectedGroups
}
test('many overlapping fixtures preserve exact coverage using only existing candidates') {
    def random = new Random(521L)
    150.times {
        def fixture = (0..<8).collectEntries { g ->
            [("group-${g}".toString()): ((0..<12).findAll { random.nextBoolean() } as Set)]
        }
        def target = fixture.values().flatten() as Set
        def result = planner.analyse(target, fixture)
        assert result.status == (target ? 'EXACT_SNAPSHOT' : 'EMPTY_SCOPE')
        if (target) {
            assert result.selectedGroups.every { fixture.containsKey(it) }
            assert (result.selectedGroups.collectMany { fixture[it] } as Set) == target
        }
    }
}

// These fakes provide only the read contracts the adapter actually consumes.
def user = { key, active = true, licensed = true -> [key:key, name:key, active:active, licensed:licensed] }
def fixture = {
    def users = [user('u1'), user('u2'), user('inactive', false), user('unlicensed', true, false)]
    def members = [alpha:[users[0], users[1], users[2]], beta:[users[1], users[3]]]
    def state = [users:users, members:members, grantNames:['alpha','beta'],
                 granted:true, anonymous:false, effective:['u1','u2'] as Set,
                 scopeReads:0, failMembership:false, changeOnSecond:false]
    def services = [
        permissions: new Expando(
            getGlobalPermission:{ key -> new Expando(isDefined:{ -> state.granted }) },
            getPermissions:{ key -> state.grantNames.collect { [group:it] } },
            getGroupNamesWithPermission:{ key -> state.grantNames as Set },
            hasPermission:{ key, u -> state.effective.contains(u.key) }),
        users: new Expando(getAllApplicationUsers:{ ->
            state.scopeReads++
            if (state.changeOnSecond && state.scopeReads == 2) state.members.beta = [state.users[0]]
            state.users }),
        groups: new Expando(groupExists:{ name -> state.members.containsKey(name) },
            getUsersInGroup:{ name ->
                if (state.failMembership) throw new IllegalStateException('PRIVATE DATA MUST NOT LEAK')
                state.members[name] }),
        roles: new Expando(hasAnyRole:{ u -> u.licensed }),
        anonymousAllowed:{ -> state.anonymous }
    ]
    [state:state, services:services]
}
def config = [permissionKey:'example.plugin.permission', appPermissionConfirmed:true,
              maxUsers:100, maxGroups:20, maxMembersPerGroup:100, showGroupNames:false]
def run = { f, changes = [:] -> planner.run(f.services, 'test-key-object', config + changes) }

test('adapter excludes inactive and unlicensed users and counts unique identities') {
    def result = run(fixture())
    assert result.status == 'EXACT_SNAPSHOT' && result.holderCount == 2
    assert result.selectedGroups == ['Group 1'] && result.unselectedGroups == ['Group 2']
}
test('default output contains neither usernames nor real group names') {
    def output = run(fixture()).toString()
    ['u1','u2','inactive','unlicensed','alpha','beta'].each { assert !output.contains(it) }
}
test('real group names require explicit operator opt-in') {
    def result = run(fixture(), [showGroupNames:true])
    assert result.selectedGroups == ['alpha'] && result.unselectedGroups == ['beta']
}
test('unknown permission fails closed') {
    def f = fixture(); f.state.granted = false
    assert run(f).status == 'BLOCKED' && run(f).reason == 'UNKNOWN_PERMISSION'
}
test('anonymous grant row fails closed') {
    def f = fixture(); f.state.grantNames.add(null)
    assert run(f).reason == 'ANONYMOUS_OR_EMPTY_GRANT'
}
test('effective anonymous access fails closed even without a null row') {
    def f = fixture(); f.state.anonymous = true
    assert run(f).reason == 'ANONYMOUS_ACCESS'
}
test('missing groups are not interpreted as empty groups') {
    def f = fixture(); f.state.grantNames.add('missing')
    assert run(f).reason == 'UNRESOLVED_GROUP'
}
test('membership read errors never leak their exception text') {
    def f = fixture(); f.state.failMembership = true
    def result = run(f)
    assert result.status == 'BLOCKED' && result.reason == 'READ_OR_API_ERROR'
    assert !result.toString().contains('PRIVATE DATA')
}
test('changed per-group membership between observations aborts even when holder count is stable') {
    def f = fixture(); f.state.changeOnSecond = true
    assert run(f).reason == 'OBSERVATIONS_CHANGED'
}
test('built-in permissions are not analysed by this app-permission example') {
    ['ADMINISTER','SYSTEM_ADMIN','USE','USER_PICKER'].each {
        assert run(fixture(), [permissionKey:it]).reason == 'BUILT_IN_PERMISSION'
    }
}
test('operator acknowledgement and configured key are mandatory') {
    assert run(fixture(), [appPermissionConfirmed:false]).reason == 'APP_SCOPE_NOT_CONFIRMED'
    assert run(fixture(), [permissionKey:'REPLACE_WITH_APP_PERMISSION_KEY']).reason == 'CONFIGURE_PERMISSION_KEY'
}
test('size caps stop analysis instead of silently truncating it') {
    assert run(fixture(), [maxUsers:1]).reason == 'USER_LIMIT'
    assert run(fixture(), [maxGroups:1]).reason == 'GROUP_LIMIT'
    assert run(fixture(), [maxMembersPerGroup:1]).reason == 'MEMBERSHIP_LIMIT'
}
test('aggregate membership cap bounds the stored observations') {
    assert run(fixture(), [maxMembershipChecks:4L]).reason == 'TOTAL_MEMBERSHIP_LIMIT'
}
test('a null anonymous-access result is not interpreted as denial') {
    def f = fixture(); f.services.anonymousAllowed = { -> null }
    assert run(f).reason == 'INVALID_BOOLEAN_RESULT'
}
test('a null application-role result is not interpreted as an unlicensed user') {
    def f = fixture(); f.services.roles = new Expando(hasAnyRole:{ u -> null })
    assert run(f).reason == 'INVALID_BOOLEAN_RESULT'
}
test('a null effective-permission result is not interpreted as denial') {
    def f = fixture(); f.services.permissions.hasPermission = { k, u -> null }
    assert run(f).reason == 'INVALID_BOOLEAN_RESULT'
}
test('unresolvable membership identities fail closed') {
    def f = fixture(); f.state.members.alpha.add([name:'unknown'])
    assert run(f).reason == 'UNRESOLVED_USER'
}
println "PASS ${passed} tests (offline; not a live Jira integration test)"
