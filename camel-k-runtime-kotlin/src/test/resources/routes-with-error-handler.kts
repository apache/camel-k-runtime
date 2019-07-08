
onException(IllegalArgumentException::class.java)
    .id("my-on-exception")
    .to("log:exception")

from("timer:tick")
    .process().message {
        m -> m.headers["MyHeader"] = "MyHeaderValue"
    }
    .to("log:info")