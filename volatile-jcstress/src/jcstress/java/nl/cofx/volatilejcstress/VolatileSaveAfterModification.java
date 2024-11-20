package nl.cofx.volatilejcstress;


import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.I_Result;

import java.util.ArrayList;
import java.util.List;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;
import static org.openjdk.jcstress.annotations.Expect.FORBIDDEN;

@JCStressTest
@State
@Outcome.Outcomes({
        @Outcome(id = "-1", expect = ACCEPTABLE, desc = "Null list"),
        @Outcome(id = "0", expect = FORBIDDEN, desc = "Empty list"),
        @Outcome(id = "42", expect = ACCEPTABLE, desc = "List containing 42"),
})
public class VolatileSaveAfterModification {

    volatile List<Integer> list;

    @Actor
    public void actor() {
        var tmpList = new ArrayList<Integer>();
        tmpList.add(42);
        list = tmpList;
    }

    @Actor
    public void observer(I_Result r) {
        var l = list;
        if (l != null) {
            if (l.isEmpty()) {
                r.r1 = 0;
            } else {
                r.r1 = l.get(0);
            }
        } else {
            r.r1 = -1;
        }
    }
}
