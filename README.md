# sqlFileORM

这是一个用 sql file 来对应 Java 对象的小框架，其主要的用途并不是去替代当前的其他框架而是对其他框架做一个补充。

在一些报表的需求当中，可能需要非常复杂的SQL进行查询，并且还需要根据不同的查询条件进行查询操作，使用该框架，你可以将查询的语句单独编写成 sql 文件模板，然后由这个框架解些出相关的SQL进行查询操作，最后，该框架会根据 SQL 中的标签将查询的结果映射成一个对象。