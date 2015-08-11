package se.sics.p2ptoolbox.election.api.rules;

import se.sics.p2ptoolbox.election.api.LEContainer;
import java.util.Comparator;

/**
 * The rule set class which is used by the
 * main application to perform various operations.
 *
 * Created by babbar on 2015-08-11.
 */
public abstract class RuleSet {


    protected Comparator<LEContainer> leComparator;


    public RuleSet(Comparator<LEContainer> comparator){
        this.leComparator = comparator;
    }

}
