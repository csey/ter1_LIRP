import java.io.PrintStream;
import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearIntExpr;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloObjectiveSense;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.DoubleParam;



public class solveur2ndproblem {


	private static final double TimeLimit = 10;
	
	//is this good?
	public static solution2 solve(instance2 instance, PrintStream printStreamSol) throws IloException {
		
		
		
		int nbSites = instance.getNbSites(); //number of sites
		int nbPeriods  = instance.getNbPeriods(); //number of periods
		double[][] listeRoutes = instance.calculateRoutesModel2();	//Calculations of routes, routes list 	
	    double [][] DeliveryCost = instance.calculateDeliveryCost(); //call the delivery costs 
		int nbRoutes = instance.getNbRoutes(); //number of routes

		int J = instance.getNbDepots();
		int T = nbPeriods;
		int R = instance.getNbRoutes();
		int I = instance.getNbClients();


		IloCplex solver = new IloCplex();
		//Boolean VARIABLES
		//cplex.intVar(0, 1, "q0_"+l);
		
		solver.setParam(DoubleParam.TiLim, TimeLimit);
		
		
		IloIntVar[] y = solver.boolVarArray(J);
		
		IloIntVar[][] z = new IloIntVar[R][T];
		for(int r=0;r<R;r++){
			for(int t=0;t<T;t++){ 
				z[r][t] = solver.boolVar(); //its not consistent 
			}
		}
		
		IloIntVar[][][] x = new IloIntVar[I][J][T];
		for(int i=0;i<I;i++){
		for(int j=0;j<J;j++){
			for(int t=0;t<T;t++){
				x[i][j][t] = solver.boolVar(); // int and boolean
			}
		}
		}
		


		double[] coutRoute = new double[nbRoutes]; // creating cost of routes
			for (int r = 0; r < R; r++) {
				coutRoute[r] = listeRoutes[r][0];
			}


		//Continuous Variables
		
		IloNumVar[][][] v = new IloNumVar[I][J][T]; //quantity delivered from j to i in t  
		for(int i=0;i<I;i++){
			for(int j=0;j<J;j++){
				for(int t=0;t<T;t++) {
					v[i][j][t] = solver.numVar(0, 99);
				}
			}
		}	
		
		
		IloNumVar[][][] q = new IloNumVar[J][R][T]; //quantity delivered to j by route r at period t
		for(int j=0;j<J;j++){
			for(int r=0;r<R;r++){
				for(int t=0;t<T;t++){
					q[j][r][t] = solver.numVar(0,99);
				}
			}
		}

		IloNumVar[][]InvClients = new IloNumVar[I][T]; // inv at clients 
		for(int i=0;i<I;i++){
			for(int t=0;t<T;t++) {
				InvClients[i][t] = solver.numVar(0,99);
			}
		}

		IloNumVar[][]InvDepots = new IloNumVar[I][T]; // inv at depots
		for(int i=0;i<J;i++){
			for(int t=0;t<T;t++){
				InvDepots[i][t] = solver.numVar(0,99);
			}
		}
		
		IloNumVar obj = solver.numVar(0, Integer.MAX_VALUE, "obj"); // objective function
		


		//to create the Alpha
		int[][] B = new int[R][J]; //if depot j is in route r
			for (int r=0;r<nbRoutes;r++){
			for(int j=0;j<J ; j++)
				B[r][j]= 0;   	
			}
		
		for (int r=0;r<nbRoutes;r++){
			for (int i=1;i<instance.getDMAX()+1;i++){
				for(int j= I; j< I+J; j++){
					if(listeRoutes[r][i]==-1)
					{
						break;
					}
					int depotOftheRoute = (int)listeRoutes[r][i] ;
					B[r][depotOftheRoute-I] = 1;
				}				
			}
		}
		
		// TODO Auto-generated constructor stub

	
		//constraint 17

		for (int j = 0; j < J; j++){
			for (int t=0;t<T;t++){
				IloLinearNumExpr expr = solver.linearNumExpr();
				for (int r=0;r<R;r++){
					expr.addTerm(B[r][j], z[r][t]);
				}
				solver.addLe(expr,1);
			}
		}

		//constraint 18 
		
		for (int r = 0; r < R; r++){
			for (int t=0;t<T;t++){
				for (int j = 0; j < J; j++){  //we add the summatory for all depots in the same route
					IloLinearNumExpr expr = solver.linearNumExpr();
					expr.addTerm(B[r][j],q[j][r][t]);
					expr.addTerm(-instance.getCapacityVehicle(), z[r][t]);
					solver.addLe(expr,0);
				}
			}
		}
		
		
	
		

		//constraint 19 If a route exist and visits depotj, then depot j must be selected   (so, y[j] must be larger than B[r][j]+z[r][t] -1) 
		
		/*	for (int r = 0; r < R; r++){ 
				for (int t=0;t<T;t++){
				IloLinearNumExpr expr = solver.linearNumExpr();
						
				expr.addTerm(1,z[r][t]);
				for (int j = 0; j < J; j++){
				expr.addTerm(- B[r][j], y[j]);
						solver.addLe(expr,0);
					}
				}	
		}*/
		
			for (int r = 0; r < R; r++){ 
				for (int t=0;t<T;t++){
					for (int j = 0; j < J; j++){
						IloLinearNumExpr expr = solver.linearNumExpr();
						expr.addTerm(1,y[j]);
						expr.addTerm(-1,z[r][t]);
						solver.addGe(expr,B[r][j]-1);
					}
				}
			}
			
			
			
			//constraint 19bis : If a depot is not open, the routes visiting this depot do not exist 
			
			for (int j = 0; j < J; j++){
					for (int t=0;t<T;t++){
					IloLinearNumExpr expr = solver.linearNumExpr();
					for (int r = 0; r < R; r++){ 	
							expr.addTerm(B[r][j],z[r][t]);
						}
					expr.addTerm(-1,y[j]);
					solver.addLe(expr,0);
					}	
			}
			

		//constraint 20 ---> Fleet constraint
		
		for (int t = 0; t < T; t++) {
			IloLinearNumExpr expr = solver.linearNumExpr();
			for (int r = 0; r < R; r++) { 
				expr.addTerm(1, z[r][t]);
			}
			solver.addLe(expr, instance.getNbVehicles());
		}

		//constraint 21
		
	 for (int i = 0; i < I; i++){
			for (int j = 0; j < J; j++){ 
				for (int t=0;t<T;t++){
					IloLinearNumExpr expr = solver.linearNumExpr();
					expr.addTerm(1, v[i][j][t] );
						expr.addTerm(-instance.getCapacityVehicle(), x[i][j][t]);
						solver.addLe(expr,0);
					}
				}
		} 

		//constraint 22 -----> No entendemos

		for (int i=0; i < I; i++)
		{
			for (int j = 0; j < J; j++)
			{
				for (int t = 0; t < nbPeriods; t++)
				{	
					IloLinearNumExpr expr = solver.linearNumExpr();
					expr.addTerm(1, v[i][j][t]);  
					double totalDemand = 0;
					for (int t2 = t; t2 < nbPeriods; t2++) 
					{
						totalDemand = totalDemand + instance.getDemand(i, t2);
					}
					expr.addTerm(-totalDemand, x[i][j][t] );
					solver.addLe(expr, 0);
				}
			}
		}

		//constraint 23
		for (int i = 0; i < I; i++){
		for (int j = 0; j < J; j++){ 
		for (int t=0;t<T;t++){
		IloLinearNumExpr expr = solver.linearNumExpr();
				expr.addTerm(1,x[i][j][t]);
						expr.addTerm(-1, y[j]);
						solver.addLe(expr,0);
					}
				}}
		
	// constraint 24 --------> between periods 0 and 1 ... revisar con olivier
		
		for (int j = 0; j < J; j++)// for all j
		{
			IloLinearNumExpr expr = solver.linearNumExpr();
			expr.addTerm(1, InvDepots[j][0]);
			for (int r = 0; r < R; r++) {
				expr.addTerm(-1, q[j][r][0]);
			}
			
			for (int i = 0; i < I; i++) 
			{
				expr.addTerm( 1, v[i][j][0]);
			}
			solver.addEq(expr, instance.getInventoryInitialDepot(j));
		}
					

		// constraint 24 --------> between periods T-1 and T
			
		for (int t = 1; t < nbPeriods; t++) { 		// for all t
			for (int j = 0; j < J; j++) {// for all j 
				IloLinearNumExpr expr = solver.linearNumExpr();
				expr.addTerm(1, InvDepots[j][t]);
				expr.addTerm(-1, InvDepots[j][t - 1]);
				for (int r = 0; r < R; r++) {
						expr.addTerm(-1, q[j][r][t]);
				}
					
				for (int i = 0; i < I; i++) // is it good ?
				{
					expr.addTerm(1, v[i][j][t]);
				}
				solver.addEq(expr, 0);
			}
		}
		

		
// constraint 25 inventory at clients, period 0 ---> 1
				
	 for (int i = 0; i < I; i++)
	{
		IloLinearNumExpr expr = solver.linearNumExpr();
		expr.addTerm(1, InvClients[i][0]);
		for (int j = 0; j < J; j++) {
				expr.addTerm(-1, v[i][j][0]);
		}
		solver.addEq(expr, instance.getInventoryInitialClient(i)-instance.getDemand(i,0));
	}
		
		
						
				// constraint 25 inventory at clients, period t-1 ---> t
				for (int i = 0; i < I; i++)
				{
					for (int j = 0; j < J; j++) {
					for (int t = 1; t < nbPeriods; t++) {

						IloLinearNumExpr expr = solver.linearNumExpr();
						expr.addTerm(1, InvClients[i][t]);
						expr.addTerm(-1, InvClients[i][t - 1]);
						
							expr.addTerm(-1, v[i][j][t]);
						
						double Dem = instance.getDemand(i, t);
						solver.addEq(expr, -Dem);
					}
					}
				}	
				
				
				
				
	//constraint 26
			 
	 for (int i=0; i < I; i++)
	 {
		 for (int t = 0; t < nbPeriods; t++){	
			 IloLinearNumExpr expr = solver.linearNumExpr();
			 expr.addTerm(1, InvClients[i][t]);  
			 double totalDemand  = 0 ;
			 for ( int t2=t; t2<nbPeriods; t2++ ){
				 totalDemand = totalDemand + instance.getDemand(i, t2);
			 }	
			 solver.addLe(expr, totalDemand);
		 }
	}
					 
	// constraint 27 , if one depot has no delivery by route r at period t,  then either route r does not exist on that period, or customer is not on this route
					
	for (int r = 0; r < R; r++) {
		for (int t = 0; t < T; t++) {
			for (int j = 0; j < J; j++) {
				IloLinearNumExpr expr = solver.linearNumExpr();
				expr.addTerm(1, q[j][r][t]);
				int bigM= B[r][j] * instance.getCapacityVehicle();
				expr.addTerm(-bigM , z[r][t]); //?? maybe mistake ==> OP : it is OK. I just changed the value of the bigM 
				solver.addLe(expr, 0);
			}
		}
	}
	
		
						
	// constraint 13: capacity constraints at depots 
			
	for (int t = 0; t < T; t++) {
		for (int j = 0; j < J; j++) {
			IloLinearNumExpr expr = solver.linearNumExpr();
			expr.addTerm(1, InvDepots[j][t]);
			int capD = instance.getCapacityDepot();
			solver.addLe(expr, capD);
		}
	}
						
							
	// constraint 14 : capacity constraints at client
	for (int t = 0; t < T; t++) {
		for (int i = 0; i < I; i++) {
			IloLinearNumExpr expr = solver.linearNumExpr();
			expr.addTerm(1, InvClients[i][t]);
			int capC = instance.getCapacityClient();
			solver.addLe(expr, capC);
		}
	}
								
						
	// constraint 15: if a depot is not selected, then its inventory is 0.
					
	for (int t = 0; t < T; t++) {
		for (int j = 0; j < J; j++) {
			IloLinearNumExpr expr = solver.linearNumExpr();
			expr.addTerm(1, InvDepots[j][t]);
			expr.addTerm(-instance.getCapacityDepot(), y[j]); // OP : one error corrected InvDepot[j][t] ==> y[j]
			solver.addLe(expr, 0);
		}
	}	
	
	// single sourcing constraint : one client is delivered by one signle depot
	
	for (int t = 0; t < T; t++) {
		for (int i = 0; i < I; i++) {
			IloLinearNumExpr expr = solver.linearNumExpr();
			for (int j = 0; j < J; j++) {
				expr.addTerm(1, x[i][j][t]);
			}
			solver.addLe(expr, 1);
		}
	}
						
			//OBJECTIVE FUNCTION
					 IloLinearNumExpr expr = solver.linearNumExpr();
						// term 1
						for (int j = 0; j < J; j++) {
							expr.addTerm(instance.getFixedCost(j), y[j]); 
						}

						// 2nd term
						for (int t = 1; t < T; t++) {
							for (int r = 0; r < R; r++) {
								expr.addTerm(coutRoute[r], z[r][t]);
							}
						}	

						// 3rd term
						for (int t = 1; t < T; t++) {
							for (int j = 0; j < J; j++) {
								for (int i = 0; i < I; i++) {
								expr.addTerm(instance.getDeliveryCost(j, i), x[i][j][t]);
							}
						}
						}
						// 4th term
						for (int t = 0; t < T; t++) {
							for (int j = 0; j < J; j++) {
								expr.addTerm(instance.getHoldingCost(j), InvDepots[j][t]);
							}
						}

						
						// 5th term inventory at clients
								for (int t = 0; t < T; t++) {
									for (int i = 0; i < I; i++) {
										expr.addTerm(instance.getHoldingCostClient(), InvClients[i][t]);
									}
								}
					
						
						//
						solver.addLe(expr, obj);
						solver.addObjective(IloObjectiveSense.Minimize, obj);
						
	
						solver.solve();
						
						System.out.println(solver.getStatus());
						System.out.println(solver.getObjValue());
						
						System.out.println();
						System.out.println();
						System.out.println("======== RESULTS  =================");
						System.out.println();
						

						System.out.print("Status of solver :   ");
						System.out.println(solver.getStatus());
						System.out.print("Objective function :   ");
						System.out.println(solver.getObjValue());
						
						
						
						
						
				///////////////////////////////////////////////////////////////////////
		
		solution2 sol = new solution2 (instance, nbRoutes);				
				
						//save the status of depots (open/closed)
							for (int j = 0; j<J; j++){
								if (solver.getValue(y[j])>0) 
								{
									sol.setOpenDepots(j,1);
								}
								else
								{
									sol.setOpenDepots(j,0);
								}
							}
						
						// save the deliveries to depots (1 if the depot is delivered, 0 otherwise)
						
						for (int r = 0; r < R; r++)	{
							for (int t = 0; t<nbPeriods; t++){
								for (int j = 0; j<J; j++){
								if (solver.getValue(q[j][r][t]) >0) {
									sol.setDeliveryDepot(j,t,1);
									}
								else {sol.setDeliveryDepot( j,t, 0);	}
							}
						}
						}
						
						// save quantities delivered to depot
						for (int t = 0; t<nbPeriods; t++){
							for (int j = 0; j<J; j++){
								for (int r = 0; r<R; r++){
									sol.setQuantityDeliveredToDepot(j,r,t,(int) solver.getValue(q[j][r][t]));
								}
							}
						}
						
						// save the inventory variables
						for (int t = 0; t<nbPeriods; t++){
							for (int j = 0; j<J; j++){
								sol.setStockDepot(j,t, solver.getValue(InvDepots[j][t]));
							}
						}
						
						// save the quantity delivered to clients
							for (int t=0;t<instance.getNbPeriods();t++){
								for (int i=0;i<instance.getNbClients();i++){
									for (int j=0;j<instance.getNbDepots(); j++){
										double value = solver.getValue(v[i][j][t]);
										//System.out.println("Period "+t+" \t depot "+j+" \t client   "+i+" \t value "+value);
										sol.setQuantityDepotToClient(i,j,t, value);
								}
							}
						}
						
						// save the quantity delivered from plant to depot 
						for (int t=0;t<instance.getNbPeriods();t++){
							for (int j=0;j<instance.getNbDepots();j++){
								for (int r=0;r<nbRoutes; r++){
									//System.out.println("Route " +r+ " Brj = "+B[r][j]+" Ari= "+A[r][i]);
									sol.setQuantityDeliveredToDepot(j,r,t,(int) solver.getValue(q[j][r][t]));
								}
							}
						}
						
							
						
						//-------------------------------------------------
						// save inventory at clients for every period t>=1
						for (int t = 0; t<nbPeriods; t++){
							for (int i = 0; i<I; i++){
								int stcli = (int) solver.getValue(InvClients[i][t]);
								sol.setStockClient(i, t, stcli);
							}
						}
							
						//save the routes performed in each period
						for (int t = 0; t<nbPeriods; t++){
							for (int r = 0; r<nbRoutes; r++){
								//System.out.println(" Period "+t+"   route "+r+"   valeur = "+solver.getValue(z[r][t])+"   coutRoute = "+coutRoute[r]);
								if (solver.getValue(z[r][t]) >0){
									sol.setListOfRoutes(r, t, 1);
								}
								else 	sol.setListOfRoutes(r, t, 0);
							}
						}
						
						// save the route costs
						for (int r = 0; r<nbRoutes; r++){
								sol.setCoutRoute(r, coutRoute[r]);
							}
						
						
						
						
						
						printStreamSol.println("--------------------------------------------");
						
						
						return sol;
					}

				}