package se.sics.ktoolbox.election;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configuration file for the election module.
 *  
 * Created by babbarshaer on 2015-04-25.
 */
public class ElectionConfig {

    private static Logger logger  = LoggerFactory.getLogger(ElectionConfig.class);
    private final long leaderLeaseTime;
    private final long followerLeaseTime;
    private final int viewSize;
    private final int convergenceRounds;
    private final double convergenceTest;
    private final int maxLeaderGroupSize;

    public ElectionConfig(long leaderLeaseTime, long followerLeaseTime, int viewSize, int convergenceRounds, double convergenceTest, int maxLeaderGroupSize) {

        this.leaderLeaseTime = leaderLeaseTime;
        this.followerLeaseTime = followerLeaseTime;
        this.viewSize = viewSize;
        this.convergenceRounds = convergenceRounds;
        this.convergenceTest = convergenceTest;
        this.maxLeaderGroupSize = maxLeaderGroupSize;
    }

    public ElectionConfig(Config config){
        
        try{
            
            this.viewSize = config.getInt("election.viewSize");
            this.maxLeaderGroupSize = config.getInt("election.maxLeaderGroupSize");
            this.leaderLeaseTime = config.getLong("election.leaderLeaseTime");
            this.followerLeaseTime = config.getLong("election.followerLeaseTime");
            this.convergenceRounds = config.getInt("election.convergenceRounds");
            this.convergenceTest = config.getDouble("election.convergenceTest");
            
            if(leaderLeaseTime >= followerLeaseTime){
                throw new RuntimeException("Leader Lease should always be less than follower lease");
            }
            
            logger.info("Election Config :  viewSize:{}, maxLeaderGroupSize:{}, leaderLeaderTime:{}, followerLeaseTime:{}, convergenceRounds:{}, convergenceTest:{}",
                    new Object[]{this.viewSize, this.maxLeaderGroupSize, this.leaderLeaseTime, this.followerLeaseTime, this.convergenceRounds, this.convergenceTest});
        }
        catch(ConfigException.Missing ex){
            logger.warn("Configuration Missing ", ex);
            throw new RuntimeException("Configuration Missing ",ex);
        }
        
        
    }

    @Override
    public String toString() {
        return "ElectionConfig{" +
                "leaderLeaseTime=" + leaderLeaseTime +
                ", followerLeaseTime=" + followerLeaseTime +
                ", viewSize=" + viewSize +
                ", convergenceRounds=" + convergenceRounds +
                ", convergenceTest=" + convergenceTest +
                ", maxLeaderGroupSize=" + maxLeaderGroupSize +
                '}';
    }

    public long getLeaderLeaseTime() {
        return leaderLeaseTime;
    }

    public long getFollowerLeaseTime() {
        return followerLeaseTime;
    }

    public int getViewSize() {
        return viewSize;
    }

    public int getConvergenceRounds() {
        return convergenceRounds;
    }

    public double getConvergenceTest() {
        return convergenceTest;
    }

    public int getMaxLeaderGroupSize() {
        return maxLeaderGroupSize;
    }
}
