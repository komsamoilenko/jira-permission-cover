def loader = new GroovyClassLoader(getClass().classLoader)
loader.parseClass(new File('src/PermissionCover.groovy'))
def planner = loader.loadClass('PermissionCover')
def holders = ['u1','u2','u3','u4','u5','u6'] as Set
def groups = [alpha:['u1','u2','u3'] as Set, beta:['u3','u4'] as Set,
              gamma:['u4','u5'] as Set, delta:['u6'] as Set]
println 'Fictional example. No Jira services or real accounts are used.'
println planner.analyse(holders, groups)
