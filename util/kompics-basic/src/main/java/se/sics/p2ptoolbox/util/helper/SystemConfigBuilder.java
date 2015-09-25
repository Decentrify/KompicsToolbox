package se.sics.p2ptoolbox.util.helper;

import com.google.common.base.Optional;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.caracaldb.Address;
import se.sics.p2ptoolbox.util.config.SystemConfig;
import se.sics.p2ptoolbox.util.network.impl.BasicAddress;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.util.Random;
import se.sics.p2ptoolbox.util.nat.NatedTrait;

/**
 * A builder pattern for loading the System Configuration.
 *
 * Created by babbar on 2015-08-25.
 */
public class SystemConfigBuilder {

    private static Logger LOG = LoggerFactory.getLogger("Config");
    private String logPrefix = "System: ";

    private Config config;
    private long seed;
    private InetAddress selfIp;
    private Integer selfPort;
    private Integer selfId;
    private Optional<Address> bootstrapAddress;
    private Optional<DecoratedAddress> aggregatorAddress;
    private Optional<NatedTrait> selfNat;

    private Random random;
    private static int BASE = 10000;
    private static int DIFF = (int) Math.pow((double) 2, (double) 16) - BASE;

    public SystemConfigBuilder(Config config) {
        this.config = config;
        try {
//          LOAD THE SEED FROM CONFIG.
            try {
                seed = config.getLong("system.seed");
            } catch (ConfigException.Missing ex) {
                Random r = new SecureRandom();
                seed = r.nextLong();
            }
            LOG.info("{}seed:{}", logPrefix, seed);
            random = new Random(seed);

//      LOAD THE SELF ADDRESS ATTRIBUTES.
            try {
                selfIp = InetAddress.getByName(config.getString("system.self.ip"));
            } catch (ConfigException.Missing ex) {
                selfIp = null;
            }
            try {
//              Port needs to be between  (10000 & 65535)
                selfPort = config.getInt("system.self.port");
            } catch (ConfigException.Missing ex) {
                setPort();
            }
            try {
                selfId = config.getInt("system.self.id");
            } catch (ConfigException.Missing ex) {
                selfId = random.nextInt();
            }
            LOG.info("{}self ip:{} port:{} id:{}", 
                    new Object[]{logPrefix, selfIp, selfPort, selfId});

//          LOAD THE BOOTSTRAP CONFIGURATION ATTRIBUTES.
            try {
                InetAddress ip = InetAddress.getByName(config.getString("caracal.address.ip"));
                int port = config.getInt("caracal.address.port");
                this.bootstrapAddress = Optional.of(new Address(ip, port, null));
            } catch (ConfigException.Missing ex) {
                this.bootstrapAddress = Optional.absent();
            }
            LOG.info("{}caracal:{}", new Object[]{logPrefix, bootstrapAddress.isPresent() ? bootstrapAddress.get() : "missing"});

//          LOAD THE OPTIONAL AGGREGATOR ATTRIBUTES.
            try {
                InetAddress aggregatorIp = InetAddress.getByName(config.getString("system.aggregator.ip"));
                int aggregatorPort = config.getInt("system.aggregator.port");
                int aggregatorId = config.getInt("system.aggregator.id");
                DecoratedAddress aggregatorAdr = new DecoratedAddress(new BasicAddress(aggregatorIp, aggregatorPort, aggregatorId));
                aggregatorAdr.addTrait(NatedTrait.open());
                this.aggregatorAddress = Optional.of(aggregatorAdr);
            } catch (Exception ex) {
                this.aggregatorAddress = Optional.absent();
            }
            LOG.info("{}aggregator:{}", new Object[]{logPrefix, aggregatorAddress.isPresent() ? aggregatorAddress.get() : "missing"});
            selfNat = Optional.absent();
        } catch (UnknownHostException ex) {
            LOG.error("{}ip exception", logPrefix);
            throw new RuntimeException(ex);
        }
    }

    public SystemConfigBuilder(long seed, InetAddress selfIp, int selfPort, int selfId) {
        this.seed = seed;
        this.selfIp = selfIp;
        this.selfPort = selfPort;
        this.selfId = selfId;
        this.bootstrapAddress = Optional.absent();
        this.aggregatorAddress = Optional.absent();
        this.selfNat = Optional.absent();
    }

    public long getSeed() {
        return seed;
    }

    public InetAddress getSelfIp() {
        return selfIp;
    }

    public Integer getSelfPort() {
        return selfPort;
    }

    public Integer getSelfId() {
        return selfId;
    }

    public Optional<Address> getBootstrapAddress() {
        return bootstrapAddress;
    }

    public Optional<DecoratedAddress> getAggregatorAddress() {
        return aggregatorAddress;
    }

    public SystemConfigBuilder setSelfIp(InetAddress selfIp) {

        this.selfIp = selfIp;
        return this;
    }

    public SystemConfigBuilder setSelfPort(int selfPort) {

        this.selfPort = selfPort;
        return this;
    }

    public SystemConfigBuilder setSelfId(int selfId) {

        this.selfId = selfId;
        return this;
    }

    public SystemConfigBuilder setBootstrapAddress(Address bootstrapAddress) {
        this.bootstrapAddress = Optional.of(bootstrapAddress);
        return this;
    }

    public SystemConfigBuilder setAggregatorAddress(DecoratedAddress aggregatorAddress) {
        this.aggregatorAddress = Optional.of(aggregatorAddress);
        return this;
    }

    public SystemConfigBuilder setSelfNat(NatedTrait selfNat) {
        this.selfNat = Optional.of(selfNat);
        return this;
    }

    public void setPort() {
        this.selfPort = (BASE + random.nextInt(DIFF));
    }
    
    public Config getConfig() {
        return config;
    }

    /**
     * Look at the values that were added to the builder by the user and
     * initiate the creation of the System Config.
     *
     * @return SystemConfig.
     */
    public SystemConfig build() {

        if ((this.selfIp == null) || (this.selfPort == null) || (this.selfId == null)) {
            throw new RuntimeException("Self Address not configured, cannot proceed with build");
        }

        DecoratedAddress selfAddress = new DecoratedAddress(new BasicAddress(selfIp, selfPort, selfId));
        if (selfNat.isPresent()) {
            selfAddress.addTrait(selfNat.get());
        }
        return new SystemConfig(config, seed, selfAddress, bootstrapAddress, aggregatorAddress);
    }
}
