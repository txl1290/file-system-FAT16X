# file-system-FAT16X
使用一个固定2G大小的文件模拟磁盘，实现文件系统FAT16X，并实现简易的shell控制台以进行文件操作。

| 命令  | 说明   |
| ---- | -----  |
| format     | 格式化磁盘 |
| mkdir      | 创建目录   |
| touch      | 创建文件   |
| ls         | 显示当前目录下的所有文件和子目录 |
| cd         | 切换目录   |
| pwd        | 查看当前路径   |
| echo       | 回显内容   |
| cat        | 显示文件内容   |
| rm	        | 删除文件或目录   |
| clear        | 清屏   |


### 问答任务
Q：MOS的实现分为了哪些部分？每一部分的职责是什么？每个部分之间的交接面是什么？

A：自底向上看，共分为4层
1. 设备层：负责磁盘扇区的读写操作。
2. 文件系统层：这部分又分为抽象文件系统层和文件系统的具体实现。
- 抽象文件系统层：定义文件操作符和文件系统基本操作集。其中关于文件的操作：open、close、read、write；关于集合的操作：listFiles、appendFile、removeFile。
- 具体文件系统层：这一层包括文件系统的协议和文件系统操作集的具体实现。
3. 文件API层：抽象了逻辑上的file、dir概念。使用继承自标准的输入输出流的文件流的方式对文件系统层进行操作，向上层应用屏蔽掉底层文件系统的实现细节。
4. 应用层：基于文件API实现具体文件系统的操作命令，以及支持管道、sshd server、scp等操作。

交接面：
其中设备层挂载到文件系统层中，文件系统层通过文件操作符来维护磁盘写入的区域。
文件API层中的File对象构造时可以指定使用的文件系统是什么，并基于绑定的文件系统封装文件逻辑上的操作。
应用层则使用文件api层提供的file、inputstream、outputstream来进行逻辑上的文件操作。

Q：如果要支持多文件系统，你会如何设计？

A：因为文件系统的实现都是基于抽象文件系统层的，所有的基础操作都会有相对应的实现，所以文件API层是完全可以支持绑定不同的文件系统。所以在构造文件API层的File对象时，传入需要使用的文件系统，这样文件API层就可以支持逻辑上File对象对应多文件系统。文件API层的实现不变的话，应用层也可以做到无感知地切换多文件系统。

Q：如果要支持多磁盘挂载，你会如何设计？

A：首先文件系统fs实现支持挂载多个磁盘，然后文件API层在初始化File对象时除了需要指定path外，还要指定磁盘区域标识，这样文件操作符就知道操作的是哪个磁盘。考虑到使用体验，这个标识可以通过制定协议包含在path中（例如c://xxx/xxx），也可以指定通过某些path和磁盘的映射关系来表达（例如 / disk1；/dev disk2）。这样在操作File对象进行文件系统读写的时候，就可以通过这个标识来区分使用哪部分磁盘来进行设备层的读写操作。

Q：如果要将系统与应用进行分离，你会如何设计？系统与应用的交接面是什么？

A：
设计：
1. 首先系统需要定义好应用的接口，所有在系统上运行的应用都要实现这个接口。例如接口定义最基础的方法，应用名name()、应用内容content()、应用执行实现run()。
2. 系统需要提供一个应用安装器installer，用于安装应用，把应用内容持久化到系统之中。
3. 系统需要提供一个应用扫描器scanner，用于扫描应用（例如在bin目录下或专门的注册表），判断哪些输入需要调起应用。
4. 系统需要提供一个应用执行器executor，用于执行应用，不同类型的应用各自实现自己的执行方法run。

交接面：
应用通过安装器持久化在系统中，应用通过扫描器加载到系统内存中，系统通过执行器执行具体的应用动作。


Q：现有的实现里，哪些功能属于应用层逻辑，可以抽离出来？如何抽离？

A：外部命令都属于应用层逻辑。
1. 首先要实现系统定义的Application接口，把对应功能的实现转换成某种语言的代码（例如java代码）放在应用的content中，并通过安装器持久化到系统的硬盘文件上，放在特定的目录下，例如bin目录。
2. 然后实现对应语言的编译器，可以对文件内容的动态编译。
3. 实现Application中的run方法，具体执行过程是接收外部传来的inputstream和参数args，结合着content中的代码内容，经过编译器得到最终的输出结果，把结果放到应用的输出流中。
4. 最后，应用的输出流与终端/管道进行交互，输出或继续执行下次操作。
