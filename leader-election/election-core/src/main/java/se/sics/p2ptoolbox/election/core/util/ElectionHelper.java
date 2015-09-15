package se.sics.p2ptoolbox.election.core.util;

import org.javatuples.Pair;
import se.sics.p2ptoolbox.election.api.LCPeerView;
import se.sics.p2ptoolbox.election.api.LEContainer;
import se.sics.p2ptoolbox.util.Container;
import se.sics.p2ptoolbox.util.network.impl.BasicAddress;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;

import java.util.*;

/**
 * Extracting common helper methods in this class.
 *
 * Created by babbar on 2015-03-31.
 */
public class ElectionHelper {


    /**
     * Update the view of the system based on the current gradient sample.
     * @param sample sample from the gradient.
     * @return updated view.
     */
    public static Map<Integer, LEContainer> addGradientSample(Collection<Container> sample){

        Map<Integer, LEContainer> containerMap = new HashMap<Integer, LEContainer>();
        for(Container container : sample){
            containerMap.put(((DecoratedAddress)container.getSource()).getBase().getId(), new LEContainer( (DecoratedAddress)container.getSource(), (LCPeerView)container.getContent()) );
        }

        return containerMap;
    }


    /**
     * Based on the change in the set in the current iteration, check if the change is less than a specified value.
     * In order words, check how much is the set remains the same and if the number is greater than the value of convergence factor.
     *
     * @param oldAddressSet set before incorporating new sample
     * @param currentAddressSet current set
     * @param convergenceFactor value determining how much change is acceptable.
     * @return if round is converged or not.
     */
    public static boolean isRoundConverged(Set<Integer> oldAddressSet, Set<Integer> currentAddressSet, double convergenceFactor){

        int oldSize = oldAddressSet.size();
        int newSize = currentAddressSet.size();

        oldAddressSet.retainAll(currentAddressSet);

        return ((oldSize == newSize) && oldAddressSet.size() >=  (int)(convergenceFactor * currentAddressSet.size()));
    }


    /**
     * Based on the collection provided recalculate the higher and lower utility views, taking into account
     * self view and the comparator provided.
     *
     * @param leContainerCollection collection
     * @param comparator comparator for collection
     * @param self self view
     * @return Pair containing lower and higher views.
     */
    public static Pair<SortedSet<LEContainer>, SortedSet<LEContainer>> getHigherAndLowerViews(Collection<LEContainer> leContainerCollection, Comparator<LEContainer> comparator, LEContainer self){

        SortedSet<LEContainer> containerSet = new TreeSet<LEContainer>(comparator);
        containerSet.addAll(leContainerCollection);

        return Pair.with(containerSet.headSet(self), containerSet.tailSet(self));
    }



}
