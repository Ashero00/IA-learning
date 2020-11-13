package group29;

import java.util.List;

import genius.core.AgentID;
import genius.core.Bid;
import genius.core.actions.Accept;
import genius.core.actions.Action;
import genius.core.actions.Offer;
import genius.core.parties.AbstractNegotiationParty;
import genius.core.parties.NegotiationInfo;
import genius.core.utility.AbstractUtilitySpace;
import genius.core.utility.AdditiveUtilitySpace;

/**
 * ExampleAgent returns the bid that maximizes its own utility for half of the negotiation session.
 * In the second half, it offers a random bid. It only accepts the bid on the table in this phase,
 * if the utility of the bid is higher than Example Agent's last bid.
 **/
public class Agent29 extends AbstractNegotiationParty {
	private final String description = "Group 29 Agent Learning";

	private Bid lastReceivedOffer; // offer on the table
	private Bid myLastOffer;
	private IaMap iaMap;

	@Override
	public void init(NegotiationInfo info) {
		super.init(info);
		AbstractUtilitySpace utilitySpace = info.getUtilitySpace();
		AdditiveUtilitySpace additiveUtilitySpace = (AdditiveUtilitySpace) utilitySpace;
		this.iaMap=new IaMap(userModel);
	}

	/**
	 * When this function is called, it is expected that the Party chooses one of the actions from the possible
	 * action list and returns an instance of the chosen action.
	 *
	 * @param list
	 * @return
	 */
	@Override
	public Action chooseAction(List<Class<? extends Action>> list) {
		// According to Stacked Alternating Offers Protocol list includes
		// Accept, Offer and EndNegotiation actions only.
		double time = getTimeLine().getTime();
		// Gets the time, running from t = 0 (start) to t = 1 (deadline).
		// The time is normalized, so agents need not be
		// concerned with the actual internal clock.

		// First half of the negotiation offering the max utility (the best agreement possible) for Example Agent
		if (time < 0.5) {
			return new Offer(this.getPartyId(), this.getMaxUtilityBid());
		} else {
			// Accepts the bid on the table in this phase,
			// if the utility of the bid is higher than Example Agent's last bid.
			if (lastReceivedOffer != null
					&& myLastOffer != null
					&& this.utilitySpace.getUtility(lastReceivedOffer) > this.utilitySpace.getUtility(myLastOffer)) {

				return new Accept(this.getPartyId(), lastReceivedOffer);
			} else {
				// Offering a random bid
				myLastOffer = generateRandomBid();
				return new Offer(this.getPartyId(), myLastOffer);
			}
		}
	}

	/**
	 * This method is called to inform the party that another NegotiationParty chose an Action.
	 * @param sender
	 * @param action
	 */
	@Override
	public void receiveMessage(AgentID sender, Action action) {
		if (action instanceof Offer) {
			lastReceivedOffer = ((Offer) action).getBid();
			iaMap.JonnyBlack(lastReceivedOffer);
		}
	}

	/**
	 * A human-readable description for this party.
	 * @return
	 */
	@Override
	public String getDescription() {
		return description;
	}

	private Bid getMaxUtilityBid() {
		try {
			return this.utilitySpace.getMaxUtilityBid();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}