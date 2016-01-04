package cc.blynk.server.admin.http.handlers;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.ipfilter.AbstractRemoteAddressFilter;

import java.net.InetSocketAddress;
import java.util.Set;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 16.12.15.
 */
@ChannelHandler.Sharable
public class IpFilterHandler extends AbstractRemoteAddressFilter<InetSocketAddress> {

    private final Set<String> allowedIPs;

    public IpFilterHandler(Set<String> allowedIPs) {
        this.allowedIPs = allowedIPs;
    }

    @Override
    protected boolean accept(ChannelHandlerContext ctx, InetSocketAddress remoteAddress) throws Exception {
        return !(allowedIPs != null && !allowedIPs.contains(remoteAddress.getAddress().getHostAddress()));
    }
}
