package sorcer.worker.requestor;

import java.io.Serializable;
import java.util.List;

import sorcer.core.context.Contexts;
import sorcer.core.context.ServiceContext;
import sorcer.service.Context;
import sorcer.service.ContextException;
import sorcer.worker.provider.Work;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class Works implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	public static Work work1, work2, work3, work4;

	static {
		work1 = new Work() {
			public Context exec(Context cxt) throws ContextException {
				String sigPrefix = ((ServiceContext)cxt).getCurrentPrefix();
				String operand1Path = "requestor/operand/1";
				String operand2Path = "requestor/operand/2";
			     if (sigPrefix != null && sigPrefix.length() > 0) {
			    	 operand1Path = sigPrefix + "/" + operand1Path;
			    	 operand2Path = sigPrefix + "/" + operand2Path;
			     } else {
			    	 sigPrefix = ""; 
			     }	        
				int arg1 = (Integer) cxt.getValue(operand1Path);
				int arg2 = (Integer) cxt.getValue(operand2Path);
				int result = arg1 + arg2;
				List<String> outPaths = ((ServiceContext)cxt).getOutPaths();
				if(outPaths.size() == 1) {
					if (sigPrefix.length() > 0)
						cxt.putValue(sigPrefix + "/" + outPaths.get(0), result);
					else
						cxt.putValue(outPaths.get(0), result);
				}
				else if (outPaths.size() > 1) {
					for (String path : outPaths) {
						String[] mpaths = Contexts.getMarkedPaths(cxt, "par|"+sigPrefix);
				        if (mpaths.length == 1) {
				        	cxt.putOutValue(mpaths[0], result);
				        	break;
				        }
					}
				} else
					cxt.putOutValue("provider/result", result);
				
				cxt.setReturnValue(result);
				return cxt;
			}
		};

		work2 = new Work() {
			public Context exec(Context cxt) throws ContextException {
				String sigPrefix = ((ServiceContext)cxt).getCurrentPrefix();
				String operand1Path = "requestor/operand/1";
				String operand2Path = "requestor/operand/2";
			     if (sigPrefix != null && sigPrefix.length() > 0) {
			    	 operand1Path = sigPrefix + "/" + operand1Path;
			    	 operand2Path = sigPrefix + "/" + operand2Path;
			     } else {
			    	 sigPrefix = ""; 
			     }			        
				int arg1 = (Integer) cxt.getValue(operand1Path);
				int arg2 = (Integer) cxt.getValue(operand2Path);
				int result = arg1 * arg2;
				List<String> outPaths = ((ServiceContext)cxt).getOutPaths();
				if(outPaths.size() == 1) {
					if (sigPrefix.length() > 0)
						cxt.putValue(sigPrefix + "/" + outPaths.get(0), result);
					else
						cxt.putValue(outPaths.get(0), result);
				}
				else if (outPaths.size() > 1) {
					for (String path : outPaths) {
						String[] mpaths = Contexts.getMarkedPaths(cxt, "par|"+sigPrefix);
				        if (mpaths.length == 1) {
				        	cxt.putOutValue(mpaths[0], result);
				        	break;
				        }
					}
				} else
					cxt.putOutValue("provider/result", result);
				
				cxt.setReturnValue(result);
				return cxt;
			}
		};

		work3 = new Work() {
			public Context exec(Context cxt) throws ContextException {
				String sigPrefix = ((ServiceContext)cxt).getCurrentPrefix();
				String operand1Path = "requestor/operand/1";
				String operand2Path = "requestor/operand/2";
			     if (sigPrefix != null && sigPrefix.length() > 0) {
			    	 operand1Path = sigPrefix + "/" + operand1Path;
			    	 operand2Path = sigPrefix + "/" + operand2Path;
			     } else {
			    	 sigPrefix = ""; 
			     }
			        
				int arg1 = (Integer) cxt.getValue(operand1Path);
				int arg2 = (Integer) cxt.getValue(operand2Path);
				int result = arg2 - arg1;
				List<String> outPaths = ((ServiceContext)cxt).getOutPaths();
				if(outPaths.size() == 1) {
					if (sigPrefix.length() > 0)
						cxt.putValue(sigPrefix + "/" + outPaths.get(0), result);
					else
						cxt.putValue(outPaths.get(0), result);
				}
				else if (outPaths.size() > 1) {
					for (String path : outPaths) {
						String[] mpaths = Contexts.getMarkedPaths(cxt, "par|"+sigPrefix);
				        if (mpaths.length == 1) {
				        	cxt.putOutValue(mpaths[0], result);
				        	break;
				        }
					}
				} else
					cxt.putOutValue("provider/result", result);
				
				cxt.setReturnValue(result);
				return cxt;
			}
		};
		
		work4 = new Work() {
			public Context exec(Context cxt) throws ContextException {
				String sigPrefix = ((ServiceContext)cxt).getCurrentPrefix();
				String operand1Path = "requestor/operand/1";
				String operand2Path = "requestor/operand/2";
				String operand3Path = "requestor/operand/3";
			     if (sigPrefix != null && sigPrefix.length() > 0) {
			    	 operand1Path = sigPrefix + "/" + operand1Path;
			    	 operand2Path = sigPrefix + "/" + operand2Path;
			    	 operand2Path = sigPrefix + "/" + operand3Path;
			     } else {
			    	 sigPrefix = ""; 
			     }	        
				int arg1 = (Integer) cxt.getValue(operand1Path);
				int arg2 = (Integer) cxt.getValue(operand2Path);
				int arg3 = (Integer) cxt.getValue(operand3Path);
				int result = Math.round((arg1 + arg2 + arg3)/3);
				List<String> outPaths = ((ServiceContext)cxt).getOutPaths();
				if(outPaths.size() == 1) {
					if (sigPrefix.length() > 0)
						cxt.putValue(sigPrefix + "/" + outPaths.get(0), result);
					else
						cxt.putValue(outPaths.get(0), result);
				}
				else if (outPaths.size() > 1) {
					for (String path : outPaths) {
						String[] mpaths = Contexts.getMarkedPaths(cxt, "par|"+sigPrefix);
				        if (mpaths.length == 1) {
				        	cxt.putOutValue(mpaths[0], result);
				        	break;
				        }
					}
				} else
					cxt.putOutValue("provider/result", result);
				
				cxt.setReturnValue(result);
				return cxt;
			}
		};
		
	}
}
