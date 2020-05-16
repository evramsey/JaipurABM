/**
 * Utility function calculator
 */
public class UtilityFunction {
    private static double a;
    private static double aPrime;
    private static double bPrime;
    private static double b;

    public static double calculateUtilityForConserverStayingConserver3Networks(double famRatio, double famDelta, double friendRatio,
                                                                               double friendDelta, double acqRatio, double exogenousTerm,
                                                                               double exogenousTermDrought){
        double aFam = updateInitialParameter(famDelta, a);
        double aFriends = updateInitialParameter(friendDelta, a);
        double utility = (a * acqRatio) + (aFam * famRatio) + (aFriends * friendRatio) + exogenousTerm + exogenousTermDrought;
        return utility;
    }

    public static double calculateUtilityForConserverStayingConserver1Network(double acqRatio, double exogenousTerm,
                                                                              double exogenousTermDrought){
        double utility = (a * acqRatio) + exogenousTerm + exogenousTermDrought;
        return utility;
    }


    public static double calculateUtilityForConserverBecomingNonConserver3Networks(double famRatio, double famDelta, double friendRatio,
                                                                                   double friendDelta, double otherRatio){
        double bFam = updateInitialParameter(famDelta, b);
        double bFriends = updateInitialParameter(friendDelta, b);
        double utility = (b * otherRatio) + (bFam * famRatio) + (bFriends * friendRatio);
        return utility;
    }


    public static double calculateUtilityForConserverBecomingNonConserver1Network(double acqRatio){
        double utility = (b * acqRatio);
        return utility;
    }

    public static double calculateUtilityForNonConserverBecomingConserver3Networks(double famRatio, double famDelta, double friendRatio,
                                                                                    double friendDelta, double otherRatio,
                                                                                   double exogenousTerm,
                                                                                   double exogenousTermDrought){
        double aPrimeFam = updateInitialParameter(famDelta, aPrime);
        double aPrimeFriends = updateInitialParameter(friendDelta, aPrime);
        double utility = (aPrime * otherRatio) + (aPrimeFam * famRatio) + (aPrimeFriends * friendRatio) + exogenousTerm
                + exogenousTermDrought;
        return utility;
    }

    public static double calculateUtilityForNonConserverBecomingConserver1Network( double acqRatio, double exogenousTerm, double exogenousTermDrought){
        double utility = (aPrime * acqRatio) + exogenousTerm + exogenousTermDrought;
        return utility;
    }

    public static double calculateUtilityForNonConserverStayingNonConserver3Networks(double famRatio, double famDelta, double friendRatio,
                                                                double friendDelta, double otherRatio){
        double bPrimeFam = updateInitialParameter(famDelta, bPrime);
        double bPrimeFriends = updateInitialParameter(friendDelta, bPrime);
        double utility = (bPrime * otherRatio) + (bPrimeFam * famRatio) + (bPrimeFriends * friendRatio);
        return utility;
    }

    public static double calculateUtilityForNonConserverStayingNonConserver1Network(double acqRatio){
        double utility = (bPrime * acqRatio);
        return utility;
    }

    //this changes parameters for both switching behavior and not switching, for both conservers and nonconservers
    public static double updateInitialParameter(double delta, double coefficient){
        return delta + coefficient;
    }

    public static void setAandBPrime(double a_bprime){
        a = a_bprime;
        bPrime = a_bprime;
    }

    public static void setBandAPrime(double b_aprime){
        b = b_aprime;
        aPrime = b_aprime;
    }

    public static double getAandBPrime(){return a;}

    public static double getBandAPrime(){return b;}

}
