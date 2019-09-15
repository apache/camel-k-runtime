import org.apache.camel.component.seda.SedaComponent

context {
    components {
        mySeda(SedaComponent) {
            // a wrong method name
            queueNumber 33
        }
    }
}

from('timer:tick')
        .to('log:info')
