package group29;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import genius.core.AgentID;
import genius.core.Bid;
import genius.core.actions.Accept;
import genius.core.actions.Action;
import genius.core.actions.Offer;
import genius.core.parties.AbstractNegotiationParty;
import genius.core.parties.NegotiationInfo;
import genius.core.uncertainty.BidRanking;
import genius.core.uncertainty.ExperimentalUserModel;
import genius.core.utility.AbstractUtilitySpace;
import genius.core.utility.AdditiveUtilitySpace;
import genius.core.utility.UncertainAdditiveUtilitySpace;

/**
 * ExampleAgent returns the bid that maximizes its own utility for half of the negotiation session.
 * In the second half, it offers a random bid. It only accepts the bid on the table in this phase,
 * if the utility of the bid is higher than Example Agent's last bid.
 **/
public class Agent29 extends AbstractNegotiationParty {
    private final String description = "Group 29 Agent Learning";

    private Bid lastReceivedOffer; // offer received from opponent
    private Bid maxBidForme;
    private IaMap iaMap;
    private AbstractUtilitySpace predictAbstractSpace;
    private AdditiveUtilitySpace predictAddtiveSpace;
    private BidRanking bidRanking;
    private double concessionValue;

    @Override
    public void init(NegotiationInfo info) {
        super.init(info);
        this.iaMap=new IaMap(userModel);
        // GeneticAlgorithm geneticAlgorithm = new GeneticAlgorithm(userModel);
        // this.predictAbstractSpace = geneticAlgorithm.geneticAlgorithm();
        UserPrefElicit userPref = new UserPrefElicit(userModel);
        predictAbstractSpace = userPref.geneticAlgorithm();
        this.predictAddtiveSpace = (AdditiveUtilitySpace)predictAbstractSpace;
        this.bidRanking = userModel.getBidRanking();
        this.maxBidForme = bidRanking.getMaximalBid();
        this.concessionValue = 0.8;
//        for (int i = 0; i < 10; i ++) {
//            Bid testBid = bidRanking.getRandomBid();
//            utilityError(testBid);
//        }
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out)));

    }

    private double utilityError(Bid bid) {
        ExperimentalUserModel e = (ExperimentalUserModel) userModel;
        UncertainAdditiveUtilitySpace realUSpace = e.getRealUtilitySpace();
        double ret = Math.abs(this.predictAddtiveSpace.getUtility(bid) - realUSpace.getUtility(bid));
        System.out.println("Utility Error: " + ret);
        return ret;
    }

    private double disagreeUtility(double disagreePercent) {
        List<Bid> bidList = userModel.getBidRanking().getBidOrder();
        int bidListSize = bidList.size();
        int disagreeIndex = (int)Math.ceil(bidListSize * disagreePercent);
        double ret = this.predictAddtiveSpace.getUtility(bidList.get(disagreeIndex));
        return ret;
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
        double time = getTimeLine().getTime();

        if (lastReceivedOffer!=null) {
            if (time < 0.3) {
                if (predictAddtiveSpace.getUtility(lastReceivedOffer) > concessionValue ||
                        predictAddtiveSpace.getUtility(lastReceivedOffer) > 0.88) {
                    return new Accept(getPartyId(), lastReceivedOffer);
                }
            } else if (time < 0.5) {
                if (predictAddtiveSpace.getUtility(lastReceivedOffer) > concessionValue ||
                        predictAddtiveSpace.getUtility(lastReceivedOffer) > 0.87) {
                    return new Accept(getPartyId(), lastReceivedOffer);
                }
            } else if (time < 0.7){
                if (predictAddtiveSpace.getUtility(lastReceivedOffer) > concessionValue ||
                        predictAddtiveSpace.getUtility(lastReceivedOffer) > 0.85) {
                    return new Accept(getPartyId(), lastReceivedOffer);
                }
            } else if (time < 0.8) {
                if (predictAddtiveSpace.getUtility(lastReceivedOffer) > concessionValue ||
                        predictAddtiveSpace.getUtility(lastReceivedOffer) > 0.83) {
                    return new Accept(getPartyId(), lastReceivedOffer);
                }
            } else if (time < 0.85) {
                if (predictAddtiveSpace.getUtility(lastReceivedOffer) > concessionValue ||
                        predictAddtiveSpace.getUtility(lastReceivedOffer) > 0.8) {
                    return new Accept(getPartyId(), lastReceivedOffer);
                }
            } else if (time < 0.9) {
                if (predictAddtiveSpace.getUtility(lastReceivedOffer) > concessionValue ||
                        predictAddtiveSpace.getUtility(lastReceivedOffer) > 0.7) {
                    return new Accept(getPartyId(), lastReceivedOffer);
                }
            } else if (time < 0.93) {
                if (predictAddtiveSpace.getUtility(lastReceivedOffer) > concessionValue ||
                        predictAddtiveSpace.getUtility(lastReceivedOffer) > iaMap.JBpredict(lastReceivedOffer)) {
                    return new Accept(getPartyId(), lastReceivedOffer);
                }
            } else if (time < 0.98) {
                if (predictAddtiveSpace.getUtility(lastReceivedOffer) > concessionValue ||
                        predictAddtiveSpace.getUtility(lastReceivedOffer) > iaMap.JBpredict(lastReceivedOffer)) {
                    return new Accept(getPartyId(), lastReceivedOffer);
                }
            } else {
                return new Accept(getPartyId(), lastReceivedOffer);
            }
        }
        return new Offer(getPartyId(), generateRandomBidAboveTarget());
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

    private Bid generateRandomBidAboveTarget() {
        long numberofPossibleBids = this.getDomain().getNumberOfPossibleBids();
        double time = getTimeLine().getTime();
        Bid randomBid;
        double util;
        double opponentUtility;

        List<Bid> randomBidList = new ArrayList<>();
        List<Double> utilList = new ArrayList<>();
        List<Double> opponentUtiList = new ArrayList<>();

        if(time < 0.5) {
            return maxBidForme;
        } else if (time < 0.4){
            for (int k = 0; k < 2*numberofPossibleBids; k ++) {
                randomBid = generateRandomBid();
                util = predictAddtiveSpace.getUtility(randomBid);
                if (util > concessionValue - 0.1 && util < concessionValue + 0.05) {
                    utilList.add(util);
                    opponentUtiList.add(iaMap.JBpredict(randomBid));
                    randomBidList.add(randomBid);
                }
            }

            // If we can not find a suitable one, just generate the max one
            if (utilList.size() == 0) {
                for (int t = 0; t < 2*numberofPossibleBids; t ++) {
                    randomBid = generateRandomBid();
                    randomBidList.add(randomBid);
                    utilList.add(predictAddtiveSpace.getUtility(randomBid));
                }
            }
            double maxUtility = Collections.max(utilList);
            int indexRandBid = utilList.indexOf(maxUtility);
            Bid suitableBid = randomBidList.get(indexRandBid);
            return suitableBid;
        } else {
            for (int k = 0; k < 3 * numberofPossibleBids; k++) {
                randomBid = generateRandomBid();
                util = predictAddtiveSpace.getUtility(randomBid);
                if (util > concessionValue - 0.05 && util < concessionValue + 0.05) {
                    utilList.add(util);
                    opponentUtiList.add(iaMap.JBpredict(randomBid));
                    randomBidList.add(randomBid);
                }
            }
            // If we can not find a suitable one, just generate the max one
            if (utilList.size() == 0) {
                for (int t = 0; t < 2 * numberofPossibleBids; t++) {
                    randomBid = generateRandomBid();
                    randomBidList.add(randomBid);
                    utilList.add(predictAddtiveSpace.getUtility(randomBid));
                }
            }
            double maxUtility = Collections.max(utilList);
            int indexRandBid = utilList.indexOf(maxUtility);
            Bid suitableBid = randomBidList.get(indexRandBid);
            return suitableBid;
        }
    }
}