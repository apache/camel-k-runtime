beans {
    bean<org.apache.commons.dbcp2.BasicDataSource>("dataSource") {
        driverClassName = "org.h2.Driver"
        url = "jdbc:h2:mem:camel"
        username = "sa"
        password = ""
    }

    bean("filterStrategy") {
        org.apache.camel.support.DefaultHeaderFilterStrategy()
    }
}
