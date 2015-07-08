CREATE SEQUENCE "public"."default_seq"
INCREMENT 1
MINVALUE 1
MAXVALUE 2147483647
START 1
CACHE 1
;

CREATE TABLE "public"."todo" (
	"id" INTEGER DEFAULT nextval('default_seq'::regclass) NOT NULL UNIQUE,
	"content" CHARACTER VARYING( 2044 ) COLLATE "pg_catalog"."default",
 PRIMARY KEY ( "id" )
 );