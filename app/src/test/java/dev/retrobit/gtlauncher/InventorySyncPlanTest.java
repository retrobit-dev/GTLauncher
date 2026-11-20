package dev.retrobit.gtlauncher;

import org.json.JSONException;
import org.junit.Test;
import static org.junit.Assert.*;

public class InventorySyncPlanTest {
    private static final String EMPTY="[]",A="[{\"name\":\"Apel\",\"quantity\":1}]",B="[{\"name\":\"Apel\",\"quantity\":2}]",C="[{\"name\":\"Pir\",\"quantity\":1}]";
    @Test public void firstSyncAndConflictsAreConservative() throws Exception {
        assertEquals(InventorySyncPlan.Action.UPLOAD,InventorySyncPlan.decide(A,null,null));
        assertEquals(InventorySyncPlan.Action.DOWNLOAD,InventorySyncPlan.decide(null,null,A));
        assertEquals(InventorySyncPlan.Action.INITIAL_CONFLICT,InventorySyncPlan.decide(EMPTY,null,A));
        assertEquals(InventorySyncPlan.Action.MATCH,InventorySyncPlan.decide(A,null,A));
        assertEquals(InventorySyncPlan.Action.UPLOAD,InventorySyncPlan.decide(B,A,A));
        assertEquals(InventorySyncPlan.Action.DOWNLOAD,InventorySyncPlan.decide(A,A,B));
        assertEquals(InventorySyncPlan.Action.BOTH_CHANGED,InventorySyncPlan.decide(B,A,C));
        assertEquals(InventorySyncPlan.Action.MATCH,InventorySyncPlan.decide(B,A,B));
        assertEquals(InventorySyncPlan.Action.REMOTE_MISSING,InventorySyncPlan.decide(A,A,null));
    }
    @Test public void normalizedEnvelopesKeepBoxAndHarvestHistory() throws Exception {
        String legacy="{\"version\":2,\"boxes\":{},\"bag\":[{\"quantity\":1,\"name\":\"Apel\"}]}";
        String modern="{\"harvests\":{},\"bag\":[{\"name\":\"Apel\",\"quantity\":1}],\"boxes\":{},\"version\":3}";
        assertEquals(InventorySyncPlan.Action.MATCH,InventorySyncPlan.decide(A,null,legacy));
        assertEquals(InventorySyncPlan.Action.MATCH,InventorySyncPlan.decide(legacy,null,modern));
        String box="{\"version\":3,\"bag\":[{\"name\":\"Apel\",\"quantity\":1}],\"boxes\":{\"home:1,1\":[{\"name\":\"Pir\",\"quantity\":1}]},\"harvests\":{\"home:2,2:apple\":\"2026-01-01\"}}";
        assertEquals(InventorySyncPlan.Action.DOWNLOAD,InventorySyncPlan.decide(modern,modern,box));
        assertEquals(InventorySyncPlan.Action.BOTH_CHANGED,InventorySyncPlan.decide(B,modern,box));
    }
    @Test public void malformedLocalRemoteAndBaselineAreRejected() throws Exception {
        for(String[] state:new String[][]{{"{broken",null,A},{A,"{broken",A},{A,null,"{broken"},{A,null,"{\"version\":3,\"bag\":[],\"boxes\":{},\"harvests\":{\"tree\":\"2026-02-30\"}}"}}){
            try {InventorySyncPlan.decide(state[0],state[1],state[2]);fail("Invalid data must not sync");}catch(JSONException expected){}
        }
    }
}
