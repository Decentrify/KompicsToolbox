package se.sics.ktoolbox.election;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Comparator;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Init;
import se.sics.ktoolbox.election.util.LCPeerView;
import se.sics.ktoolbox.election.rules.CohortsRuleSet;
import se.sics.ktoolbox.election.rules.LCRuleSet;
import se.sics.ktoolbox.util.network.KAddress;

/**
 * Base Init class for the core classes involved in leader election.
 * Created by babbar on 2015-04-04.
 */
public class ElectionInit<T extends ComponentDefinition> extends  Init <T> {

    public final KAddress selfAddress;
    public final LCPeerView initialView;
    public final PublicKey publicKey;
    public final PrivateKey privateKey;
    public final Comparator<LCPeerView> comparator;
    public final LCRuleSet lcRuleSet;
    public final CohortsRuleSet cohortsRuleSet;

    public ElectionInit(KAddress selfAddress,
                        LCPeerView initialView,
                        PublicKey publicKey,
                        PrivateKey privateKey,
                        Comparator<LCPeerView> comparator,
                        LCRuleSet lcRuleSet, CohortsRuleSet cohortsRuleSet){

        this.selfAddress = selfAddress;
        this.initialView = initialView;
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.comparator = comparator;
        this.lcRuleSet = lcRuleSet;
        this.cohortsRuleSet = cohortsRuleSet;
    }
}
