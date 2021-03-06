package se.sics.ktoolbox.election.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.ktoolbox.election.msg.LeaseCommitUpdated;
import se.sics.ktoolbox.election.msg.Promise;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import se.sics.ktoolbox.util.network.KAddress;

/**
 * Keeps Track of the promises responses
 * that a node receives from the peers in the system.
 *
 * Created by babbarshaer on 2015-03-29.
 */
public class PromiseResponseTracker {

    private UUID electionRoundId;
    
    private boolean isPromiseAccepted;
    private int convergenceCounter;
    
    private boolean isCommitAccepted;
    
    private Logger logger = LoggerFactory.getLogger(PromiseResponseTracker.class);
    private List<KAddress> leaderGroupInformation;
    private List<Promise.Response> promiseResponseCollection;
    private List<LeaseCommitUpdated.Response> leaseResponseCollection;
    
    public PromiseResponseTracker(){
        
        isPromiseAccepted = true;
        isCommitAccepted = true;
        convergenceCounter = 0;
    }

    /**
     * A new promise round has started, therefore the tracking of responses needs to be reset.
     * @param promiseRoundCounter promise round msgId.
     */
    public void startTracking(UUID promiseRoundCounter, Collection<KAddress> leaderGroupInformation){

        this.isPromiseAccepted = true;
        this.convergenceCounter=0;
        this.isCommitAccepted = true;
        
        this.electionRoundId = promiseRoundCounter;
        this.leaderGroupInformation = new ArrayList<KAddress>();
        this.leaderGroupInformation.addAll(leaderGroupInformation);
        
        this.promiseResponseCollection = new ArrayList<Promise.Response>();
        this.leaseResponseCollection = new ArrayList<LeaseCommitUpdated.Response>();
    }

    /**
     * Add the voting response and update the associated state with the 
     * voting round that is being tracked.
     *
     * @param response Voting response from peer.
     */
    public int addPromiseResponseAndGetSize (Promise.Response response){
        
        if(electionRoundId == null || !response.electionRoundId.equals(electionRoundId)){
            logger.warn("Received a response that is not currently tracked.");
            return 0;
        }

        isPromiseAccepted = isPromiseAccepted && response.acceptCandidate;
        convergenceCounter = response.isConverged ? (convergenceCounter + 1)  : convergenceCounter;
        
        promiseResponseCollection.add(response);
        
        return promiseResponseCollection.size();
    }

    
    public int addLeaseCommitResponseAndgetSize (LeaseCommitUpdated.Response response){
        
        if(electionRoundId == null || !response.electionRoundId.equals(electionRoundId)){
            logger.warn("Received a response that is not currently tracked.");
        }
        
        isCommitAccepted = isCommitAccepted && response.isCommit;
        leaseResponseCollection.add(response);
        
        return leaseResponseCollection.size();
    }
    
    /**
     * Size of the current leader group.
     * @return leader group size.
     */
    public int getLeaderGroupInformationSize(){
        
        int size = 0;
        if(this.leaderGroupInformation != null){
            size = leaderGroupInformation.size();
        }
        
        return size;
    }


    public UUID getRoundId(){
        return this.electionRoundId;
    }

    /**
     * Completely reset the state.
     *  
     */
    public void resetTracker(){
        
        this.electionRoundId = null;
        this.convergenceCounter = 0;
        
        this.isPromiseAccepted = false;
        this.isCommitAccepted = false;
        
        this.leaderGroupInformation = null;
        this.promiseResponseCollection = null;
        this.leaseResponseCollection  = null;
    }


    /**
     * When the class is tracking voting round, the method returns  
     * that is the node accepted as leader at that point.
     * <br/>
     * In case the tracking information has been reset for some reason, the method simply
     * returns <b>false</b> to prevent any violation of safety.
     *
     * @return Is node accepted as leader till that point.
     */
    public boolean isAccepted(){
        return isPromiseAccepted;
    }


    /**
     * The tracker main class keeps information about the lease commit accepts sent by the 
     * nodes in the system.
     *
     * @return Is node accepted as leader.
     */
    public boolean isLeaseCommitAccepted() {
        return this.isCommitAccepted;
    }
    
    /**
     * Iterate over the responses 
     * @return Nodes Converged Count.
     */
    public int getConvergenceCount(){
        return convergenceCounter;
    }


    /**
     * Return the information about the group that the node
     * trying to assert itself as leader thinks should be the leader group.
     *
     * @return LeaderGroupInformation.
     */
    public List<KAddress> getLeaderGroupInformation(){
        return this.leaderGroupInformation;
    }
    
}
