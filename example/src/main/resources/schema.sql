create table demo_orders (
    id bigint primary key,
    customer varchar(100) not null,
    total decimal(12, 2) not null
);
