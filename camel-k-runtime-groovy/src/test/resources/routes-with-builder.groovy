@Grab('org.apache.commons:commons-inexistent:1.0')

import static org.apache.camel.k.groovy.Builder.*

from('timer:clock')
    .to('log:info')
