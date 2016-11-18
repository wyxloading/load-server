package com.loading.server;

import java.util.List;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.util.CharsetUtil;

public class WebSocketHandshakeHandler extends MessageToMessageDecoder<Object> {
	
	private final WebSocketClientHandshaker handshaker;
	private ChannelPromise handshakeFuture;
	
	public WebSocketHandshakeHandler(WebSocketClientHandshaker handshaker) {
		this.handshaker = handshaker;
	}
	
	public ChannelFuture handshakeFuture() {
		return handshakeFuture;
	}
	
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		handshaker.handshake(ctx.channel());
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		System.out.println("WebSocket Client disconnected!");
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		cause.printStackTrace();
		if(!handshakeFuture.isDone()) {
			handshakeFuture.setFailure(cause);
		}
		ctx.close();
	}

	@Override
	public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
		handshakeFuture = ctx.newPromise();
	}

	@Override
	protected void decode(ChannelHandlerContext ctx, Object msg, List<Object> out) throws Exception {
		Channel ch = ctx.channel();
		if(!handshaker.isHandshakeComplete()) {
			handshaker.finishHandshake(ch, (FullHttpResponse) msg);
			System.out.println("WebSocket Client connected!");
			handshakeFuture.setSuccess();
			return;
		}
		
		if(msg instanceof FullHttpResponse) {
			FullHttpResponse response = (FullHttpResponse) msg;
			throw new IllegalStateException("Unexpected FullHttpResponse (getStatus=" + response.status() + 
					", content=" + response.content().toString(CharsetUtil.UTF_8));
		}
		
		WebSocketFrame frame = (WebSocketFrame) msg;
		if(frame instanceof PongWebSocketFrame) {
			System.out.println("WebSocket Client received pong");
		} else if(frame instanceof CloseWebSocketFrame) {
			System.out.println("WebSocket Client received closing");
			ch.close();
		} else if(frame instanceof BinaryWebSocketFrame) {
			BinaryWebSocketFrame binaryFrame = (BinaryWebSocketFrame) frame;
			out.add(binaryFrame.content());
		}
	}

}
