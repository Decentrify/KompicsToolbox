package se.sics.p2ptoolbox.election.example.simulator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.Init;
import se.sics.kompics.KompicsEvent;
import se.sics.kompics.network.Address;
import se.sics.kompics.network.Msg;
import se.sics.kompics.network.Transport;
import se.sics.p2ptoolbox.election.example.main.HostManagerComp;
import se.sics.p2ptoolbox.election.example.msg.AddPeers;
import se.sics.p2ptoolbox.simulator.SimulationContext;
import se.sics.p2ptoolbox.simulator.cmd.NetworkOpCmd;
import se.sics.p2ptoolbox.simulator.cmd.impl.StartNodeCmd;
import se.sics.p2ptoolbox.simulator.dsl.adaptor.Operation;
import se.sics.p2ptoolbox.simulator.dsl.adaptor.Operation1;
import se.sics.p2ptoolbox.util.network.impl.BasicAddress;
import se.sics.p2ptoolbox.util.network.impl.BasicContentMsg;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;
import se.sics.p2ptoolbox.util.network.impl.DecoratedHeader;

import java.util.Set;

/**
 * Operations for testing the leader election algorithm.
 *
 * Created by babbar on 2015-04-01.
 */
public class LeaderElectionOperations{

    private static Logger logger = LoggerFactory.getLogger(LeaderElectionOperations.class);


    public static Operation1<StartNodeCmd, Long> startHostManager = new Operation1<StartNodeCmd, Long>() {

        public StartNodeCmd generate(final Long id){

            return new StartNodeCmd<HostManagerComp, DecoratedAddress>() {

                long nodeId = LeaderOperationsHelper.getNodeId(id);

                @Override
                public Integer getNodeId() {
                    return (int) nodeId;
                }

                @Override
                public int bootstrapSize() {
                    return 0;
                }

                @Override
                public Class getNodeComponentDefinition() {
                    return HostManagerComp.class;
                }

                @Override
                public Init<HostManagerComp> getNodeComponentInit(DecoratedAddress aggregatorServer, Set<DecoratedAddress> bootstrapNodes) {
                    return LeaderOperationsHelper.generateComponentInit(nodeId, aggregatorServer, bootstrapNodes);
                }

                @Override
                public DecoratedAddress getAddress() {
                    return LeaderOperationsHelper.getBasicAddress(nodeId);
                }

            };
        }
    };




    public static Operation<NetworkOpCmd> updatePeersAddress = new Operation<NetworkOpCmd>() {

        @Override
        public NetworkOpCmd generate() {

            return new NetworkOpCmd() {

                @Override
                public void beforeCmd(SimulationContext context) {

                }

                @Override
                public boolean myResponse(KompicsEvent response) {
                    return false;
                }

                @Override
                public void validate(SimulationContext context, KompicsEvent response) throws ValidationException {

                }

                @Override
                public void afterValidation(SimulationContext context) {

                }

                @Override
                public Msg getNetworkMsg(Address origin) {

                    DecoratedAddress destination = LeaderOperationsHelper.getUniqueAddress();
                    DecoratedHeader<DecoratedAddress> header = new DecoratedHeader<DecoratedAddress>((DecoratedAddress)origin, destination, Transport.UDP);

                    AddPeers addPeers = new AddPeers(LeaderOperationsHelper.getPeersAddressCollection());
                    BasicContentMsg<DecoratedAddress, DecoratedHeader<DecoratedAddress>, AddPeers> addPeersMessage = new BasicContentMsg<DecoratedAddress, DecoratedHeader<DecoratedAddress>, AddPeers>(header,addPeers);

                    return addPeersMessage;
                }


            };
        }
    };


    public static Operation1<StartNodeCmd, Long> startTrueLeader = new Operation1<StartNodeCmd, Long>() {

        public StartNodeCmd generate(final Long id){

            return new StartNodeCmd<HostManagerComp, DecoratedAddress>() {

                int nodeId = Integer.MIN_VALUE;

                @Override
                public Integer getNodeId() {
                    return nodeId;
                }

                @Override
                public int bootstrapSize() {
                    return 0;
                }

                @Override
                public Class getNodeComponentDefinition() {
                    return HostManagerComp.class;
                }

                @Override
                public Init<HostManagerComp> getNodeComponentInit(DecoratedAddress aggregatorServer, Set<DecoratedAddress> bootstrapNodes) {
                    return LeaderOperationsHelper.generateComponentInit(nodeId, aggregatorServer, bootstrapNodes);
                }

                @Override
                public DecoratedAddress getAddress() {
                    return LeaderOperationsHelper.getBasicAddress(nodeId);
                }

            };
        }
    };











}
