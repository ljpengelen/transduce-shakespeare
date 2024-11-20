package nl.cofx.volatilejcstress;


import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.I_Result;

import java.util.ArrayList;
import java.util.List;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;
import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE_INTERESTING;

@JCStressTest
@State
@Outcome.Outcomes({
        @Outcome(id = "-1", expect = ACCEPTABLE, desc = "Null list"),
        @Outcome(id = "-2", expect = ACCEPTABLE_INTERESTING, desc = "Non-empty list without item"),
        @Outcome(id = "0", expect = ACCEPTABLE_INTERESTING, desc = "Empty list"),
        @Outcome(id = "42", expect = ACCEPTABLE, desc = "List containing 42"),
})
public class VolatileSaveBeforeModification {

    volatile List<Integer> list;

    @Actor
    public void actor() {
        list = new ArrayList<>();
        list.add(42);
    }

    @Actor
    public void observer(I_Result r) {
        var l = list;
        if (l != null) {
            if (l.isEmpty()) {
                r.r1 = 0;
            } else {
                try {
                    var value = l.get(0);
                    r.r1 = value != null ? value : -1;
                } catch (Exception e) {
                    r.r1 = -2;
                }
            }
        } else {
            r.r1 = -1;
        }
    }
}
