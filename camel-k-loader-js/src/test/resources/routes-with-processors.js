
from('direct:function')
    .process(function(e) { e.getMessage().setBody('function') });

from('direct:arrow')
    .process(e => { e.getMessage().setBody('arrow') });

