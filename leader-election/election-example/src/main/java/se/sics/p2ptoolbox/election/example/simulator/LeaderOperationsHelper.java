package se.sics.p2ptoolbox.election.example.simulator;

import com.google.common.base.Optional;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.p2ptoolbox.election.core.ElectionConfig;
import se.sics.p2ptoolbox.election.example.main.LCPComparator;
import se.sics.p2ptoolbox.election.example.main.HostManagerComp;
import se.sics.p2ptoolbox.util.config.SystemConfig;
import se.sics.p2ptoolbox.util.network.impl.BasicAddress;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import se.sics.p2ptoolbox.util.helper.SystemConfigBuilder;

/**
 * Helper Class for the Leader Election Protocol Operations Simulation.
 *
 * Created by babbar on 2015-04-01.
 */
public class LeaderOperationsHelper {

    private static Logger logger = LoggerFactory.getLogger(LeaderOperationsHelper.class);
    private static ConsistentHashtable<Long> ringNodes = new ConsistentHashtable<Long>();
    private static Long identifierSpaceSize = (long) 3000;
    private static long seed = 54321;
    private static Map<Long, DecoratedAddress> addressCollection = new HashMap<Long, DecoratedAddress>();
    private static LinkedList<DecoratedAddress> copy = new LinkedList<DecoratedAddress>();

    private static InetAddress ip = null;
    private static SystemConfig systemConfig;
    private static ElectionConfig electionConfig;
    
    static {
        try {
            
            ip = InetAddress.getLocalHost();
            Config config = ConfigFactory.load("application.conf");
            electionConfig = new ElectionConfig(config);
            
        } catch (UnknownHostException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }



    public static HostManagerComp.HostManagerCompInit generateComponentInit (long id, DecoratedAddress aggregatorAddress, Set<DecoratedAddress> bootstrapNodes){

        logger.info(" Generating address for peer with id: {} ", id);

        BasicAddress basic = new BasicAddress(ip, 9999, (int)id);
        DecoratedAddress selfAddress = new DecoratedAddress(basic);
        
        addressCollection.put(id, selfAddress);
        copy.add(selfAddress);
        //TODO Alex - caracal bootstrap missing
        systemConfig = new SystemConfigBuilder(seed, selfAddress.getIp(), selfAddress.getPort(), selfAddress.getId(), null, Optional.of(aggregatorAddress)).build();
        HostManagerComp.HostManagerCompInit init = new HostManagerComp.HostManagerCompInit(systemConfig, electionConfig, new LCPComparator());

        return init;
    }

    public static long getNodeId(long id){

        Long successor = ringNodes.getNode(id);

        while (successor != null && successor.equals(id)) {
            id = (id + 1) % identifierSpaceSize;
            successor = ringNodes.getNode(id);
        }

        return id;
    }


    public static Collection<DecoratedAddress> getPeersAddressCollection(){
        return addressCollection.values();
    }


    /**
     * Simply cycle through the linked list.
     *
     * @return Address in rotation.
     */
    public static DecoratedAddress getUniqueAddress(){

        if(copy == null || copy.size() == 0){
            throw new IllegalStateException("No entries in the list to return to");
        }

        
        DecoratedAddress currentAddress = copy.removeFirst();
        copy.addLast(currentAddress);
        
        return currentAddress;
    }




    public static DecoratedAddress getBasicAddress(long id){
        return addressCollection.get(id);
    }


}
