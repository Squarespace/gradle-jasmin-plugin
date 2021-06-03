.class public com/example/HelloWorld
.super java/lang/Object

.method public <init>()V
    aload_0
    invokenonvirtual java/lang/Object/<init>()V
    return
.end method

.method public static sayHello()Ljava/lang/String;
    .limit stack 2
    
    getstatic java/lang/System/out Ljava/io/PrintStream;
    ldc "Hello from Jasmin!"
    invokevirtual java/io/PrintStream/println(Ljava/lang/String;)V
    
	invokestatic com/example/Util/message()Ljava/lang/String;
    areturn
.end method


