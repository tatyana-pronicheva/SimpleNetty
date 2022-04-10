import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import java.nio.charset.StandardCharsets;

public class Server {
    private final int port;

    public static void main(String[] args) throws InterruptedException {
        new Server(9000).start();
    }

    public Server(int port) {
        this.port = port;
    }

    public void start() throws InterruptedException {
        //ThreadPool отвечающий за инициализацию новых подключений
        NioEventLoopGroup bossGroup = new NioEventLoopGroup(1);
        //ThreadPool обслуживающий всех активных клиентов
        NioEventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap server = new ServerBootstrap();
            server
                    .group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class) //Используем серверную версию сокета
                    .childHandler(new ChannelInitializer<NioSocketChannel>() {
                        @Override
                        protected void initChannel(NioSocketChannel ch) {
                            ch.pipeline().addLast(
                                    new ChannelInboundHandlerAdapter() {

                                        String messageStorage = "";

                                        @Override
                                        public void channelRegistered(ChannelHandlerContext ctx) {
                                            System.out.println("channelRegistered");
                                        }

                                        @Override
                                        public void channelUnregistered(ChannelHandlerContext ctx) {
                                            System.out.println("channelUnregistered");
                                        }

                                        @Override
                                        public void channelActive(ChannelHandlerContext ctx) {
                                            System.out.println("channelActive");
                                        }

                                        @Override
                                        public void channelInactive(ChannelHandlerContext ctx) {
                                            System.out.println("channelInactive");
                                        }

                                        @Override
                                        public void channelRead(ChannelHandlerContext ctx, Object msg) {
                                            System.out.println("channelRead");
                                            final ByteBuf m = (ByteBuf) msg;
                                            for (int i = m.readerIndex(); i < m.writerIndex(); i++) {
                                                if (m.getByte(i) == 13) {
                                                    System.out.print("Получен символ переноса строки");
                                                    ByteBuf reply = Unpooled.wrappedBuffer(messageStorage.getBytes(StandardCharsets.UTF_8));
                                                    ctx.writeAndFlush(reply);
                                                    messageStorage = "";
                                                } else {
                                                    System.out.println((char) m.getByte(i));
                                                    messageStorage += (char) m.getByte(i);
                                                }
                                            }
                                        }

                                        @Override
                                        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                                            System.out.println("Cause exception");
                                            cause.printStackTrace();
                                            ctx.close(); //Инициируем отключение клиента
                                        }
                                    }

                            );
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            Channel channel = server.bind(port).sync().channel();

            System.out.println("Server started");
            channel.closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
