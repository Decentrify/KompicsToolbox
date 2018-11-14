package se.sics.ktoolbox.election.event;

import java.util.UUID;
import se.sics.kompics.util.Identifier;
import se.sics.ktoolbox.election.util.LCPeerView;

/**
 * Event from application in the system, indicating
 * updated value of self view.
 * <p>
 * Created by babbarshaer on 2015-03-27.
 */
public class ViewUpdate implements ElectionEvent {

  public final Identifier eventId;
  public final LCPeerView selfPv;
  public final UUID electionRoundId;

  public ViewUpdate(Identifier eventId, UUID electionRoundId, LCPeerView pv) {
    this.eventId = eventId;
    this.selfPv = pv;
    this.electionRoundId = electionRoundId;
  }

  @Override
  public Identifier getId() {
    return eventId;
  }
}
