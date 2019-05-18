

from('servlet:/test')
    .convertBodyTo(String.class)
    .to('log:info')