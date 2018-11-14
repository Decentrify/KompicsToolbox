package se.sics.ktoolbox.election.msg;

import java.util.UUID;
import se.sics.kompics.util.Identifier;
import se.sics.ktoolbox.election.event.ElectionEvent;
import se.sics.ktoolbox.election.util.LCPeerView;
import se.sics.ktoolbox.util.network.KAddress;

/**
 * Promise Message Object which is
 * sent between the nodes in the system as part
 * of the Leader Election Protocol.
 * <p>
 * Created by babbarshaer on 2015-03-29.
 */
public class Promise {

  public static class Request implements ElectionEvent {

    public final Identifier msgId;
    public final LCPeerView leaderView;
    public final KAddress leaderAddress;
    public final UUID electionRoundId;

    public Request(Identifier msgId, KAddress leaderAddress, LCPeerView leaderView, UUID electionRoundId) {
      this.msgId = msgId;
      this.leaderAddress = leaderAddress;
      this.leaderView = leaderView;
      this.electionRoundId = electionRoundId;
    }

    @Override
    public Identifier getId() {
      return msgId;
    }

    public Response answer(boolean acceptCandidate, boolean isConverged, UUID electionRoundId) {
      return new Response(this, acceptCandidate, isConverged, electionRoundId);
    }
  }

  public static class Response implements ElectionEvent {

    public final Identifier msgId;
    public final boolean acceptCandidate;
    public final boolean isConverged;
    public final UUID electionRoundId;

    protected Response(Identifier msgId, boolean acceptCandidate, boolean isConverged, UUID electionRoundId) {
      this.msgId = msgId;
      this.acceptCandidate = acceptCandidate;
      this.isConverged = isConverged;
      this.electionRoundId = electionRoundId;
    }

    private Response(Request req, boolean acceptCandidate, boolean isConverged, UUID electionRoundId) {
      this(req.getId(), acceptCandidate, isConverged, electionRoundId);
    }

    @Override
    public Identifier getId() {
      return msgId;
    }
  }
}
