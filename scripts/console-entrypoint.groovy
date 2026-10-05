import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.permission.GlobalPermissionKey
import com.atlassian.jira.application.ApplicationRoleManager
import com.atlassian.jira.user.ApplicationUser

// Read README first. Configure locally, never commit your real key or output.
final String PERMISSION_KEY = 'REPLACE_WITH_APP_PERMISSION_KEY'
final boolean APP_PERMISSION_CONFIRMED = false
final boolean SHOW_GROUP_NAMES = false
final int MAX_USERS = 10000
final int MAX_GROUPS = 500
final int MAX_MEMBERS_PER_GROUP = 20000
final long MAX_MEMBERSHIP_CHECKS = 2000000L

def permissionManager = ComponentAccessor.globalPermissionManager
def key = GlobalPermissionKey.of(PERMISSION_KEY)
def services = [
    permissions:permissionManager,
    groups:ComponentAccessor.groupManager,
    users:ComponentAccessor.userManager,
    roles:ComponentAccessor.getComponent(ApplicationRoleManager),
    // Explicit cast selects the ApplicationUser overload, including for anonymous access.
    anonymousAllowed:{ -> permissionManager.hasPermission(key, (ApplicationUser) null) }
]
return PermissionCover.run(services, key, [
    permissionKey:PERMISSION_KEY,
    appPermissionConfirmed:APP_PERMISSION_CONFIRMED,
    showGroupNames:SHOW_GROUP_NAMES,
    maxUsers:MAX_USERS,
    maxGroups:MAX_GROUPS,
    maxMembersPerGroup:MAX_MEMBERS_PER_GROUP,
    maxMembershipChecks:MAX_MEMBERSHIP_CHECKS
])
