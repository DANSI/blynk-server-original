package cc.blynk.server.admin.http.handlers;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.ipfilter.AbstractRemoteAddressFilter;
import io.netty.handler.ipfilter.IpFilterRuleType;
import io.netty.handler.ipfilter.IpSubnetFilterRule;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 16.12.15.
 */
@ChannelHandler.Sharable
public class IpFilterHandler extends AbstractRemoteAddressFilter<InetSocketAddress> {

    private static final Logger log = LogManager.getLogger(IpFilterHandler.class);

    private final Set<String> allowedIPs = new HashSet<>();
    private final Set<IpSubnetFilterRule> rules = new HashSet<>();

    public IpFilterHandler(String[] allowedIPs) {
        if (allowedIPs == null) {
            return;
        }
        for (String allowedIP : allowedIPs) {
            if (allowedIP.contains("/")) {
                String[] split = allowedIP.split("/");
                String ip = split[0];
                int cidr = Integer.parseInt(split[1]);
                this.rules.add(new IpSubnetFilterRule(ip, cidr, IpFilterRuleType.ACCEPT));
            } else {
                this.allowedIPs.add(allowedIP);
            }
        }
    }

    public boolean accept(ChannelHandlerContext ctx) {
        return accept(ctx, (InetSocketAddress) ctx.channel().remoteAddress());
    }

    @Override
    public boolean accept(ChannelHandlerContext ctx, InetSocketAddress remoteAddress) {
        if (allowedIPs.size() == 0 && rules.size() == 0) {
            log.error("allowed.administrator.ips property is empty. Access restricted.");
            return false;
        }

        String remoteHost = remoteAddress.getAddress().getHostAddress();
        if (allowedIPs.contains(remoteHost)) {
            return true;
        }

        for (IpSubnetFilterRule rule : rules) {
            if (rule.matches(remoteAddress)) {
                return true;
            }
        }

        log.error("Access restricted for {}.", remoteHost);
        return false;
    }
}
