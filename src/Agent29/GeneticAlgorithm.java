package group29;

import genius.core.Bid;
import genius.core.issue.Issue;
import genius.core.issue.IssueDiscrete;
import genius.core.issue.Value;
import genius.core.issue.ValueDiscrete;
import genius.core.uncertainty.AdditiveUtilitySpaceFactory;
import genius.core.uncertainty.BidRanking;
import genius.core.uncertainty.UserModel;
import genius.core.utility.AbstractUtilitySpace;
import genius.core.utility.AdditiveUtilitySpace;
import genius.core.utility.EvaluatorDiscrete;

import java.util.*;

public class GeneticAlgorithm {
    private UserModel userModel;
    private Random random = new Random(); //用于生成随机数

    private List<AbstractUtilitySpace> population = new ArrayList<AbstractUtilitySpace>();  //用于存放所有的累加效用空间population
    private int popSize = 500;          //每一个population的总数
    private int maxIterNum = 170;       //最大迭代的次数
    private double mutationRate = 0.04; //变异几率

    //构造函数。
    public GeneticAlgorithm(UserModel userModel) {
        this.userModel = userModel;
    }

    public AbstractUtilitySpace geneticAlgorithm() {
        // 初始化种群
        for (int i = 0; i < popSize * 4; i ++) {
            population.add(getRandomChromosome());  // 此时种群里有2000个。后面会择优筛选掉1500个
        }

        // 重复迭代 maxiterNum 次
        for (int num = 0; num < maxIterNum; num ++) {
            List <Double> fitnessList = new ArrayList<>();

            for (int i = 0; i < population.size(); i ++) {
                fitnessList.add(getFitness(population.get(i)));
            }

            // 轮盘选择这些种群
            population = select(population, fitnessList, popSize);

            // crossover的时候考虑变异
            for (int i = 0; i < popSize*0.1; i ++) {
                AdditiveUtilitySpace father = (AdditiveUtilitySpace) population.get(random.nextInt(popSize));
                AdditiveUtilitySpace mother = (AdditiveUtilitySpace) population.get(random.nextInt(popSize));
                AbstractUtilitySpace child = crossover(father, mother);
                population.add(child);
            }
        }

        // 对最后一个种群只挑选最好的，作为最后的答案
        List<Double> lastFitnessList = new ArrayList<> ();
        for (AbstractUtilitySpace i: population) {
            lastFitnessList.add(getFitness(i));
        }
        double bestFitness = Collections.max(lastFitnessList);
        int index = lastFitnessList.indexOf(bestFitness);
        getFitness(population.get(index));

        return population.get(index);
    }

    private double getFitness(AbstractUtilitySpace abstractUtilitySpace){
        // 1.先从userModel中取出bidRanking列表
        BidRanking bidRanking = userModel.getBidRanking();

        // 先把bidRanking存放在一个列表中。不然的话，不能使用索引去取值
        List<Bid> bidRankingStore = new ArrayList<>();
        for (Bid bid: bidRanking){
            bidRankingStore.add(bid);
        }

        // 2.我们要单独写一个bidList去存放bidRanking去防止计算量过大
        List<Bid> bidList = new ArrayList<>();

        // 如果bid量小于400
        if (bidRanking.getSize() <= 400) {
            for (Bid bid:bidRanking) {
                bidList.add(bid);
            }
        } else {
            int sampleGap = 1 + bidRanking.getSize() / 400;
            for (int i = 0; i < bidRanking.getSize(); i += sampleGap) {
                bidList.add(bidRankingStore.get(i));
            }
        }

        // 计算在当前空间下，每个bidRanking的实际效用是多少。并且放入utilityList中。
        // 注意，此时的utilityList的索引和bidRanking的索引是相同的。我们需要利用这个存放在TreeMap中

        List<Double> utilityList = new ArrayList<>();


        for (Bid bid:bidList) {
            utilityList.add(abstractUtilitySpace.getUtility(bid));
        }

        TreeMap<Integer, Double> utilityRank = new TreeMap<>();

        for (int i = 0; i < utilityList.size(); i ++) {
            utilityRank.put(i, utilityList.get(i));
        }

        // 4. 此时我们需要根据TreeMap的值进行排序（值中存放的是效用值）
        Comparator<Map.Entry<Integer, Double>> valueComparator = Comparator.comparingDouble(Map.Entry::getValue);
        // map 转换成 list 进行排序
        List<Map.Entry<Integer, Double>> listRank = new ArrayList<>(utilityRank.entrySet());

        Collections.sort(listRank, valueComparator);

        int error = 0;
        for (int i = 0; i < listRank.size(); i ++) {
            int gap = Math.abs(listRank.get(i).getKey()-i);
            error += gap * gap;
        }

        // 6.对数思想，需要的迭代次数最少，可以使用交叉熵
        double score = 0.0f;
        double x = error / (Math.pow(listRank.size(), 3));
        double theta = -15*Math.log(x + 0.00001f);
        score = theta;
        System.out.println("Error:" + error);

        return score;
    }

    // 存在疑问
    private AbstractUtilitySpace getRandomChromosome() {
        AdditiveUtilitySpaceFactory additiveUtilitySpaceFactory = new AdditiveUtilitySpaceFactory(userModel.getDomain());
        List<Issue> issues = additiveUtilitySpaceFactory.getDomain().getIssues();
        for (Issue issue: issues) {
            additiveUtilitySpaceFactory.setWeight(issue, random.nextDouble());
            IssueDiscrete values = (IssueDiscrete) issue;
            for (Value value:values.getValues()) {
                additiveUtilitySpaceFactory.setUtility(issue, (ValueDiscrete) value, random.nextDouble());
            }
        }
        additiveUtilitySpaceFactory.normalizeWeights();
        return additiveUtilitySpaceFactory.getUtilitySpace();
    }

    // select 算法，基于轮盘赌算法和精英策略
    private List<AbstractUtilitySpace> select(List<AbstractUtilitySpace> population, List<Double> fitnessList,
                                              int popSize) {
        int eliteNumber = 2; // 精英数目
        List<AbstractUtilitySpace> nextPopulation = new ArrayList<>();

        // 复制就是为了保留原来的结果，只对副本进行修改
        List<Double> copyFitnessList = new ArrayList<>();
        for (int i = 0; i < fitnessList.size(); i ++) {
            copyFitnessList.add(fitnessList.get(i));
        }

        for (int i = 0; i < eliteNumber; i ++) {
            double maxFitness = Collections.max(copyFitnessList);
            double minFitness = Collections.min(copyFitnessList);
            int index = copyFitnessList.indexOf(maxFitness);
            nextPopulation.add(population.get(index));

            // 这里将找到的最大值设置为最小值，方便找到下一个最大值
            copyFitnessList.set(index, minFitness);
        }

        // 先保存所有的fitness
        double sumFitness = 0.0;
        for(int i = 0; i < eliteNumber; i ++){
            sumFitness += fitnessList.get(i);
        }

        // 轮盘赌算法
        for(int i = 0; i < popSize-eliteNumber; i++){
            double randNum = random.nextDouble() * sumFitness;
            double sum = 0.0;
            for(int j = 0; j < population.size(); j++) {
                sum += fitnessList.get(j);
                if(sum > randNum){
                    nextPopulation.add(population.get(j));
                    break;
                }
            }
        }

        return nextPopulation;
    }

    private AbstractUtilitySpace crossover(AdditiveUtilitySpace father, AdditiveUtilitySpace mother) {
        double wFather;
        double wMother;
        double wUnion;
        double ShiftValue;
        double ShiftStep = 0.35; // 偏向的比例

        AdditiveUtilitySpaceFactory additiveUtilitySpaceFactory = new AdditiveUtilitySpaceFactory(userModel.getDomain());
        List<IssueDiscrete> issueList = additiveUtilitySpaceFactory.getIssues();
        for (IssueDiscrete issue: issueList) {
            wFather = father.getWeight(issue);
            wMother = mother.getWeight(issue);

            wUnion = (wFather + wMother) / 2;
            ShiftValue = ShiftStep*Math.abs(wFather-wMother);
            if (Math.random() > 0.5) {
                double wChild = wUnion + ShiftValue;
                additiveUtilitySpaceFactory.setWeight(issue, wChild);
            } else {
                double wChild = wUnion - ShiftValue;
                // 避免值过小
                if (wChild < 0.01) wChild = 0.01;
                additiveUtilitySpaceFactory.setWeight(issue, wChild);
            }
            // 变异
            if (random.nextDouble() < mutationRate) additiveUtilitySpaceFactory.setWeight(issue, random.nextDouble());

            // 对issue中的value做出同样的操作

            for (ValueDiscrete value:issue.getValues()) {
                wFather = ((EvaluatorDiscrete) father.getEvaluator(issue)).getDoubleValue(value);
                wMother = ((EvaluatorDiscrete) mother.getEvaluator(issue)).getDoubleValue(value);
                wUnion = (wFather+wMother) / 2;
                ShiftValue = ShiftStep*Math.abs(wFather-wMother);
                if (Math.random() > 0.5) {
                    double wChild = wUnion + ShiftValue;
                    additiveUtilitySpaceFactory.setUtility(issue, value, wChild);
                } else {
                    double wChild = wUnion - ShiftValue;
                    // 避免值过小
                    if (wChild < 0.01) wChild = 0.01;
                    additiveUtilitySpaceFactory.setUtility(issue, value, wChild);
                }
                // 变异
                if (random.nextDouble() < mutationRate) additiveUtilitySpaceFactory.setUtility(issue, value,
                        random.nextDouble());
            }
        }
        additiveUtilitySpaceFactory.normalizeWeights();
        return additiveUtilitySpaceFactory.getUtilitySpace();
    }
}
