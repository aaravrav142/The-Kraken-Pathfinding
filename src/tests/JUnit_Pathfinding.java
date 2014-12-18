package tests;

import java.util.Random;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import pathfinding.AStar;
import robot.RobotChrono;
import robot.RobotReal;
import smartMath.Vec2;
import strategie.GameState;
import enums.ConfigInfo;
import enums.GameElementNames;
import enums.PathfindingNodes;
import enums.ServiceNames;
import exceptions.PathfindingException;
import exceptions.PathfindingRobotInObstacleException;

/**
 * Tests unitaires de la recherche de chemin.
 * @author pf
 *
 */

public class JUnit_Pathfinding extends JUnit_Test {

	private AStar pathfinding;
	private GameState<RobotChrono> state_chrono;
	private GameState<RobotReal> state;
	
	@SuppressWarnings("unchecked")
	@Before
    public void setUp() throws Exception {
        super.setUp();
    	config.set(ConfigInfo.DUREE_PEREMPTION_OBSTACLES, 100);
        pathfinding = (AStar) container.getService(ServiceNames.A_STAR);
		state = (GameState<RobotReal>)container.getService(ServiceNames.REAL_GAME_STATE);
		state_chrono = state.cloneGameState();
	}

	@Test(expected=PathfindingRobotInObstacleException.class)
    public void test_robot_dans_obstacle() throws Exception
    {
		state_chrono.robot.setPosition(new Vec2(80, 80));
    	state_chrono.gridspace.creer_obstacle(new Vec2(80, 80));
    	pathfinding.computePath(state_chrono, PathfindingNodes.values()[0], false);
    }

	@Test(expected=PathfindingException.class)
    public void test_obstacle() throws Exception
    {
		state_chrono.robot.setPosition(new Vec2(80, 80));
		state_chrono.gridspace.creer_obstacle(PathfindingNodes.values()[0].getCoordonnees());
    	pathfinding.computePath(state_chrono, PathfindingNodes.values()[0], false);
    }

	@Test
    public void test_brute_force() throws Exception
    {
    	for(PathfindingNodes i: PathfindingNodes.values())
        	for(PathfindingNodes j: PathfindingNodes.values())
        		if(!j.is_high_precision_point()) // on n'arrive jamais dans un point de haute précision
        		{
        			state_chrono = state.cloneGameState();
        			state_chrono.robot.setPosition(i.getCoordonnees());
        			pathfinding.computePath(state_chrono, j, true);
        		}
    }
	
	@Test
    public void test_element_jeu_disparu() throws Exception
    {
    	// une fois ce verre pris, le chemin est libre
    	state_chrono.table.setDone(GameElementNames.VERRE_3);
    	state_chrono.robot.setPositionPathfinding(PathfindingNodes.BAS_GAUCHE);
    	pathfinding.computePath(state_chrono, PathfindingNodes.COTE_MARCHE_GAUCHE, false);
    }

	@Test(expected=PathfindingException.class)
    public void test_element_jeu_disparu_2() throws Exception
    {
		// Exception car il y a un verre sur le passage
		state_chrono.robot.setPositionPathfinding(PathfindingNodes.BAS_GAUCHE);
    	pathfinding.computePath(state_chrono, PathfindingNodes.COTE_MARCHE_GAUCHE, false);
    }
	
	@Test
    public void test_element_jeu_disparu_3() throws Exception
    {
		// Pas d'exception car on demande au pathfinding de passer sur les éléments de jeux.
		state_chrono.robot.setPositionPathfinding(PathfindingNodes.BAS_GAUCHE);
    	pathfinding.computePath(state_chrono, PathfindingNodes.COTE_MARCHE_GAUCHE, true);
    }

	@Test
    public void test_peremption_pendant_trajet() throws Exception
    {
    	state_chrono.robot.setPosition(new Vec2(80, 80));
		state_chrono.gridspace.creer_obstacle(PathfindingNodes.values()[0].getCoordonnees());
    	pathfinding.computePath(state_chrono, PathfindingNodes.values()[0], true);
    }

//	@Test
    public void test_benchmark() throws Exception
    {
		Random randomgenerator = new Random();
		for(int k = 0; k < 100000; k++)
		{
			PathfindingNodes i = PathfindingNodes.values()[randomgenerator.nextInt(PathfindingNodes.values().length)];
			PathfindingNodes j = PathfindingNodes.values()[randomgenerator.nextInt(PathfindingNodes.values().length)];
			config.setDateDebutMatch(); // afin d'avoir toujours une haute précision
			state_chrono = state.cloneGameState();
			state_chrono.robot.setPosition(i.getCoordonnees());
			pathfinding.computePath(state_chrono, j, true);
		}
    }

}
