package se.sics.ktoolbox.aggregator.global.api.system;

/**
 * Processed window indicating that the designer has
 * processed the raw window and the result is contained in the window.
 *
 * Created by babbar on 2015-09-03.
 */
public abstract class ProcessedWindow<T> {

    int loc;
    int max;
    T entity;

    public ProcessedWindow(int loc, int max, T entity){

        this.loc = loc;
        this.max = max;
        this.entity = entity;
    }

    /**
     * Get the maximum windows processed.
     * @return maximum processed.
     */
    public int getMax() {
        return max;
    }

    /**
     * Get the location of the processed window.
     * @return location.
     */
    public int getLocation() {
        return loc;
    }

    /**
     * Get the entity associated with the processed window which is
     * the result of the processing.
     * @return
     */
    public T getEntity() {
        return entity;
    }
}
