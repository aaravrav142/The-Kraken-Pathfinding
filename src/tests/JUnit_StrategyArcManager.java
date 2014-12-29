package tests;

import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import pathfinding.StrategyArcManager;
import robot.RobotChrono;
import robot.RobotReal;
import scripts.Decision;
import scripts.Script;
import scripts.ScriptManager;
import strategie.GameState;
import enums.PathfindingNodes;
import enums.ScriptNames;
import enums.ServiceNames;

/**
 * Tests unitaires du StrategyArcManager
 * @author pf
 *
 */

public class JUnit_StrategyArcManager extends JUnit_Test {

	private StrategyArcManager strategyarcmanager;
	private GameState<RobotChrono> state;
	private ScriptManager scriptmanager;

	@Before
    public void setUp() throws Exception {
        super.setUp();
        strategyarcmanager = (StrategyArcManager) container.getService(ServiceNames.STRATEGY_ARC_MANAGER);
		@SuppressWarnings("unchecked")
		GameState<RobotReal> realstate = (GameState<RobotReal>)container.getService(ServiceNames.REAL_GAME_STATE);
		state = realstate.cloneGameState();
        scriptmanager = (ScriptManager) container.getService(ServiceNames.SCRIPT_MANAGER);
	}
	
	@Test
	public void test_iterator1() throws Exception
	{
    	state.robot.va_au_point_pathfinding(PathfindingNodes.BAS_DROITE, null);
		strategyarcmanager.reinitHashes();
		strategyarcmanager.reinitIterator(state);
		Assert.assertTrue(strategyarcmanager.hasNext(state));
		Decision d = strategyarcmanager.next();
		Assert.assertEquals(ScriptNames.ScriptClap, d.script_name);
		Assert.assertEquals(0, d.version);
		Assert.assertTrue(strategyarcmanager.hasNext(state));
		d = strategyarcmanager.next();
		Assert.assertEquals(ScriptNames.ScriptClap, d.script_name);
		Assert.assertEquals(1, d.version);
		Assert.assertTrue(strategyarcmanager.hasNext(state));
		d = strategyarcmanager.next();
		Assert.assertEquals(ScriptNames.ScriptTapis, d.script_name);
		Assert.assertEquals(0, d.version);
		Assert.assertTrue(!strategyarcmanager.hasNext(state));
	}

	@Test
	public void test_iterator2() throws Exception
	{
    	Script s = scriptmanager.getScript(ScriptNames.ScriptTapis);
    	state.robot.va_au_point_pathfinding(s.point_entree(0), null);
    	s.agit(0, state);
    	state.robot.va_au_point_pathfinding(s.point_sortie(0), null);
    	Script s2 = scriptmanager.getScript(ScriptNames.ScriptAttente);
    	for(int i = 0; i < 5; i++)
    	{
    		s2.agit(0, state);
			strategyarcmanager.reinitHashes();
			strategyarcmanager.reinitIterator(state);
			Assert.assertTrue(strategyarcmanager.hasNext(state));
			Decision d = strategyarcmanager.next();
			Assert.assertEquals(ScriptNames.ScriptClap, d.script_name);
			Assert.assertEquals(0, d.version);
			Assert.assertTrue(strategyarcmanager.hasNext(state));
			d = strategyarcmanager.next();
			Assert.assertEquals(ScriptNames.ScriptClap, d.script_name);
			Assert.assertEquals(1, d.version);
			Assert.assertTrue(!strategyarcmanager.hasNext(state));
    	}
	}

	@Test
	public void test_iterator3() throws Exception
	{
    	Script s = scriptmanager.getScript(ScriptNames.ScriptClap);
    	state.robot.va_au_point_pathfinding(s.point_entree(0), null);
    	s.agit(0, state);
    	state.robot.va_au_point_pathfinding(s.point_sortie(0), null);
    	Script s2 = scriptmanager.getScript(ScriptNames.ScriptAttente);
    	for(int i = 0; i < 5; i++)
    	{
    		s2.agit(0, state);
			strategyarcmanager.reinitHashes();
			strategyarcmanager.reinitIterator(state);
			Assert.assertTrue(strategyarcmanager.hasNext(state));
			Decision d = strategyarcmanager.next();
			Assert.assertEquals(ScriptNames.ScriptClap, d.script_name);
			Assert.assertEquals(1, d.version);
			Assert.assertTrue(strategyarcmanager.hasNext(state));
			d = strategyarcmanager.next();
			Assert.assertEquals(ScriptNames.ScriptTapis, d.script_name);
			Assert.assertEquals(0, d.version);
			Assert.assertTrue(!strategyarcmanager.hasNext(state));
    	}
	}

	@Test
	public void test_iterator4() throws Exception
	{
    	Script s = scriptmanager.getScript(ScriptNames.ScriptClap);
    	state.robot.va_au_point_pathfinding(s.point_entree(1), null);
    	s.agit(1, state);
    	state.robot.va_au_point_pathfinding(s.point_sortie(1), null);
    	Script s2 = scriptmanager.getScript(ScriptNames.ScriptAttente);
    	for(int i = 0; i < 5; i++)
    	{
    		s2.agit(0, state);
			strategyarcmanager.reinitHashes();
			strategyarcmanager.reinitIterator(state);
			Assert.assertTrue(strategyarcmanager.hasNext(state));
			Decision d = strategyarcmanager.next();
			Assert.assertEquals(ScriptNames.ScriptClap, d.script_name);
			Assert.assertEquals(0, d.version);
			Assert.assertTrue(strategyarcmanager.hasNext(state));
			d = strategyarcmanager.next();
			Assert.assertEquals(ScriptNames.ScriptTapis, d.script_name);
			Assert.assertEquals(0, d.version);
			Assert.assertTrue(!strategyarcmanager.hasNext(state));
    	}
	}

	@Test
	public void test_iterator5() throws Exception
	{
    	Script s = scriptmanager.getScript(ScriptNames.ScriptClap);
    	state.robot.va_au_point_pathfinding(s.point_entree(0), null);
    	s.agit(0, state);
    	state.robot.va_au_point_pathfinding(s.point_entree(1), null);
    	s.agit(1, state);
    	state.robot.va_au_point_pathfinding(s.point_sortie(1), null);
    	Script s2 = scriptmanager.getScript(ScriptNames.ScriptAttente);
    	for(int i = 0; i < 5; i++)
    	{
    		s2.agit(0, state);
			strategyarcmanager.reinitHashes();
			strategyarcmanager.reinitIterator(state);
			Assert.assertTrue(strategyarcmanager.hasNext(state));
			Decision d = strategyarcmanager.next();
			Assert.assertEquals(ScriptNames.ScriptTapis, d.script_name);
			Assert.assertEquals(0, d.version);
			Assert.assertTrue(!strategyarcmanager.hasNext(state));
    	}
	}

	@Test
	public void test_iterator6() throws Exception
	{
    	Script s = scriptmanager.getScript(ScriptNames.ScriptClap);
    	Script s1 = scriptmanager.getScript(ScriptNames.ScriptTapis);
    	state.robot.va_au_point_pathfinding(s.point_entree(0), null);
    	s.agit(0, state);
    	state.robot.va_au_point_pathfinding(s1.point_entree(0), null);
    	s1.agit(0, state);
    	state.robot.va_au_point_pathfinding(s1.point_sortie(0), null);
    	Script s2 = scriptmanager.getScript(ScriptNames.ScriptAttente);
    	for(int i = 0; i < 5; i++)
    	{
    		s2.agit(0, state);
			strategyarcmanager.reinitHashes();
			strategyarcmanager.reinitIterator(state);
			Assert.assertTrue(strategyarcmanager.hasNext(state));
			Decision d = strategyarcmanager.next();
			Assert.assertEquals(ScriptNames.ScriptClap, d.script_name);
			Assert.assertEquals(1, d.version);
			Assert.assertTrue(!strategyarcmanager.hasNext(state));
    	}
	}

	@Test
	public void test_distanceTo() throws Exception
	{
    	Script s = scriptmanager.getScript(ScriptNames.ScriptClap);
    	state.robot.va_au_point_pathfinding(s.point_entree(0), null);
    	ArrayList<PathfindingNodes> chemin = new ArrayList<PathfindingNodes>();
    	chemin.add(s.point_entree(0));

    	GameState<RobotChrono> state2 = state.cloneGameState();
		Decision d = new Decision(chemin, ScriptNames.ScriptClap, 0);
		strategyarcmanager.distanceTo(state2, d);
		
		s.agit(0, state);
		state.robot.setPositionPathfinding(s.point_sortie(0));
		
		Assert.assertEquals(state.robot.getHash(), state2.robot.getHash());

		strategyarcmanager.reinitHashes();
		strategyarcmanager.reinitIterator(state);
		Assert.assertTrue(strategyarcmanager.hasNext(state));
		d = strategyarcmanager.next();
		Assert.assertEquals(ScriptNames.ScriptClap, d.script_name);
		Assert.assertEquals(1, d.version);
		Assert.assertTrue(strategyarcmanager.hasNext(state));
		d = strategyarcmanager.next();
		Assert.assertEquals(ScriptNames.ScriptTapis, d.script_name);
		Assert.assertEquals(0, d.version);
		Assert.assertTrue(!strategyarcmanager.hasNext(state));

		strategyarcmanager.reinitHashes();
		strategyarcmanager.reinitIterator(state2);
		Assert.assertTrue(strategyarcmanager.hasNext(state2));
		d = strategyarcmanager.next();
		Assert.assertEquals(ScriptNames.ScriptClap, d.script_name);
		Assert.assertEquals(1, d.version);
		Assert.assertTrue(strategyarcmanager.hasNext(state2));
		d = strategyarcmanager.next();
		Assert.assertEquals(ScriptNames.ScriptTapis, d.script_name);
		Assert.assertEquals(0, d.version);
		Assert.assertTrue(!strategyarcmanager.hasNext(state2));

	}
	
}
