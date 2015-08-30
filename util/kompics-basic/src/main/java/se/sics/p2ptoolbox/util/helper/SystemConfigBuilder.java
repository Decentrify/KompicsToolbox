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

/**
 * A builder pattern for loading the System Configuration.
 *
 * Created by babbar on 2015-08-25.
 */
public class SystemConfigBuilder {


    private Config config;
    private long seed;
    private InetAddress selfIp;
    private Integer selfPort;
    private Integer selfId;
    private Address bootstrapAddress;
    private Optional<DecoratedAddress> aggregatorAddress;

    private static Logger LOG  = LoggerFactory.getLogger(SystemConfig.class);

    private Random random;
    private static int BASE = 10000;
    private static int DIFF  = (int)Math.pow((double)2, (double)16) - BASE;

    public SystemConfigBuilder(Config config) {

        this.config = config;


        try {

//          LOAD THE SEED FROM CONFIG.
            try {
                seed = config.getLong("system.seed");
            }
            catch (ConfigException.Missing ex) {

                Random r = new SecureRandom();
                seed = r.nextLong();
            }

            random = new Random(seed);


//      LOAD THE SELF ADDRESS ATTRIBUTES.

            try {
                selfIp = InetAddress.getByName(config.getString("system.self.ip"));
            }
            catch (ConfigException.Missing ex) {

                LOG.trace("Self Ip address is null.");
                selfIp = null;
            }
            LOG.trace("Self ip address is {}", selfIp);

            try {
//              Port needs to be between  (10000 & 65535)
                selfPort = config.getInt("system.self.port");
            }
            catch(ConfigException.Missing ex){
                setPort();
            }
            LOG.trace("Self address port is {}", selfPort);

            try {
                selfId = config.getInt("system.self.id");
            }
            catch (ConfigException.Missing ex) {
                selfId = random.nextInt();
            }
            LOG.trace("Self Identifier is : {}", selfId);

//          LOAD THE BOOTSTRAP CONFIGURATION ATTRIBUTES.
            try {

                InetAddress ip = InetAddress.getByName(config.getString("caracal.address.ip"));
                int port = config.getInt("caracal.address.port");
                this.bootstrapAddress = new Address(ip, port, null);
            }

            catch(ConfigException.Missing ex){
                throw new RuntimeException("Caracal Location Missing", ex);
            }
            LOG.info("Caracal Client address is: {}", this.bootstrapAddress);


//          LOAD THE OPTIONAL AGGREGATOR ATTRIBUTES.

            try {

                InetAddress aggregatorIp = InetAddress.getByName(config.getString("system.aggregator.ip"));
                int aggregatorPort = config.getInt("system.aggregator.port");
                int aggregatorId = config.getInt("system.aggregator.id");
                this.aggregatorAddress = Optional.of(new DecoratedAddress(new BasicAddress(aggregatorIp, aggregatorPort, aggregatorId)));
            }

            catch(Exception ex) {
                this.aggregatorAddress  = Optional.absent();
            }
            LOG.info("Aggregator address is: {}", this.aggregatorAddress);
        }

        catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }


    public SystemConfigBuilder(long seed, InetAddress selfIp, int selfPort, int selfId, Address bootstrapAddress, Optional<DecoratedAddress> aggregatorAddress){

        this.seed = seed;
        this.selfIp = selfIp;
        this.selfPort = selfPort;
        this.selfId = selfId;
        this.bootstrapAddress = bootstrapAddress;
        this.aggregatorAddress = aggregatorAddress;
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

        this.bootstrapAddress = bootstrapAddress;
        return this;
    }

    public SystemConfigBuilder setAggregatorAddress(Optional<DecoratedAddress> aggregatorAddress) {

        this.aggregatorAddress = aggregatorAddress;
        return this;
    }


    public void setPort(){
        this.selfPort = (BASE + random.nextInt(DIFF)) ;
    }

    /**
     * Look at the values that were added to the builder
     * by the user and initiate the creation of the System Config.
     *
     * @return SystemConfig.
     */
    public SystemConfig build() {

        if( (this.selfIp == null) || (this.selfPort == null) || (this.selfId == null)){
            throw new RuntimeException("Self Address not configured, cannot proceed with build");
        }

        if(bootstrapAddress == null){
            throw new RuntimeException("Caracal Client not configured, cannot proceed with build.");
        }
        DecoratedAddress selfAddress = new DecoratedAddress(new BasicAddress(selfIp, selfPort, selfId));
        return new SystemConfig(seed, bootstrapAddress, selfAddress, aggregatorAddress, config);
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

    public Address getBootstrapAddress() {
        return bootstrapAddress;
    }

    public Optional<DecoratedAddress> getAggregatorAddress() {
        return aggregatorAddress;
    }
}
