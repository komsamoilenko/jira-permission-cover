// Compile and execute the ACTUAL assembled script with minimal Jira API boundary doubles.
// This checks packaging/dispatch, not binary compatibility with real Jira libraries.
def root = new File(args ? args[0] : '.').canonicalFile
def loader = new GroovyClassLoader(getClass().classLoader)
loader.parseClass('''package com.atlassian.jira.component
class ComponentAccessor {
    static Object globalPermissionManager, groupManager, userManager, roleManager
    static Object getComponent(Class type) { roleManager }
}''')
loader.parseClass('''package com.atlassian.jira.permission
class GlobalPermissionKey {
    String key
    static GlobalPermissionKey of(String value) { new GlobalPermissionKey(key:value) }
}''')
loader.parseClass('package com.atlassian.jira.application; class ApplicationRoleManager {}')
loader.parseClass('''package com.atlassian.jira.user
class ApplicationUser { String key, name; boolean active = true }
''')
def component = loader.loadClass('com.atlassian.jira.component.ComponentAccessor')
def userType = loader.loadClass('com.atlassian.jira.user.ApplicationUser')
def u = userType.newInstance(); u.key = 'fictional-key'; u.name = 'fictional-user'
component.globalPermissionManager = new Expando(
    getGlobalPermission:{ k -> new Expando(isDefined:{ -> true }) },
    getPermissions:{ k -> [[group:'fictional-group']] },
    getGroupNamesWithPermission:{ k -> ['fictional-group'] },
    hasPermission:{ k, person -> person != null })
component.groupManager = new Expando(groupExists:{ name -> name == 'fictional-group' },
                                    getUsersInGroup:{ name -> [u] })
component.userManager = new Expando(getAllApplicationUsers:{ -> [u] })
component.roleManager = new Expando(hasAnyRole:{ person -> true })
def source = new File(root, 'dist/plan-permission-cover.groovy').getText('UTF-8')
def shell = new GroovyShell(loader)
def blocked = shell.evaluate(source)
assert blocked.status == 'BLOCKED' && blocked.reason == 'CONFIGURE_PERMISSION_KEY'
// Simulate only the two documented operator configuration edits in a separate class loader.
def configured = source.replace("'REPLACE_WITH_APP_PERMISSION_KEY'", "'fictional.plugin.permission'")
                       .replace('APP_PERMISSION_CONFIRMED = false', 'APP_PERMISSION_CONFIRMED = true')
def result = new GroovyShell(loader).evaluate(configured)
assert result.status == 'EXACT_SNAPSHOT'
assert result.holderCount == 1 && result.selectedGroups == ['Group 1']
assert !result.toString().contains('fictional')
println 'PASS assembled entry point: defaults block; configured fictional services produce a redacted exact snapshot'
