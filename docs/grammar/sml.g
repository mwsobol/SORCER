grammar sml;

/* <PROVIDER-RULES> */
srvSignature : prvSignature | opSignature | bldrSignature | 'sig' '(' srvSignature ',' signatureOp ')' ;
prvSignature : 'sig' '(' ( sigName ',' )? prvSpec ')' ;
opSignature  : 'sig' '(' ( sigName ',' )? opSpec ',' prvSpec ')' ;
prvSpec : (srvType | 'type' '(' srvTypeName ')') (',' matchTypes)? (',' (prvId))? (',' prvDeployment)? | bldrSignature | prvInstance ;
bldrSignature : 'sig' '('( sigName ',' )? classSelector ','classType ')' ;
matchTypes : 'types' '(' (interfaceType ',')* interfaceType ')' ; 
opSpec  : (selector | signatureOp) ( ',' dataContext )? (',' srvResult)? (',' inputConnector)? (',' outputConnector)? ;
inputConnector : 'inConn' '(' (mapEntry ',')* mapEntry ')' ;
outputConnector : 'outConn' '(' (mapEntry ',')* mapEntry ')' ;

srvType : classType | interfaceType ;

signatureOp : 'op' '(' selector (',' opArg)* ')' | 'op' '(' opSignature ')' ;
opArg : accessType | flowType | provisionable ;
accessType : 'Access.PUSH' | 'Access.PULL' ;
provisionable : 'Provision.YES' | 'Provision.NO' ;
prvId : 'srvName' '(' serviceName (',' 'locators' '('(locatorName',')+ ')')? ((',' groupName)+)? ')' | 'prvName' '(' providerName ')' ;
srvResult : 'result' '(' pathName? (',' inputPaths)? (',' outputPaths)? (',' dataContext)? ')' ;
prvDeployment : 'deploy' '(' 'implementation' '(' providerClassName ')' 
               'classpath' '(' jarName* ')' 
			   'codebase' '(' jarName* 'configuration' '('configName ')' 'maintain' '(' intNumber ')' 'idle' '(' intNumber ')' ')' ;
inputPaths : 'inPaths' '(' srvPath+ ')' ;
outputPaths : 'outPaths' '(' srvPath+ ')' ;

/* <PROVIDER-SERVICES> */

prvInstance : 'prv' '(' srvSignature ')' ;


/* <REQUESTS> */

srvRequest : srvSignature | contextEntry | srvMogram ;


/* <ENTRIES> */

annotatedPath : 'path' '(' pathName ( ',' pathTag)? ')' ;
srvPath : pathName | annotatedPath ;
entOp : 'inVal' | 'outVal' | 'inoutVal' | 'dbVal' ;

dataEntry : entOp '(' srvPath ',' value ')' ;

procEntry : ('ent' '(' pathName ',' (srvEvaluator | srvInvoker ( ',' srvModel)?) ')' | sigEntry | lambdaEntry) ;

srvRoutine : contextEntry | srvInvoker | srvEvaluator ;

sigEntry : 'ent' '(' pathName? ',' opSignature ')' ;

mapEntry : 'ent' '(' fromPathName ',' toPathName ')' ;

lambdaEntry : 'lambda' '(' pathName ',' entrycallableLambdaExpression ')'
			| 'lambda' '(' pathName ',' serviceLambdaExpression',' srvArgs ')'
			| 'lambda' '(' pathName ',' callableLambdaExpression',' srvArgs ')'
			| 'lambda' '(' pathName ',' clientLambdaExpression',' srvArgs ')'
			| 'lambda' '(' pathName ',' valueCallableLambdaExpression',' srvArgs ')' ;

srvEntry : ('ent' '(' pathName ',' opSignature ( ',' srvModel)? ( ',' cxtSelector)? ')') 
			| ('ent' '(' pathName ',' srvRoutine ')') | ('ent' '(' pathName ',' srvMogram ')') ;

cxtSelector : selector '(' (componentName',' )? pathName+ ')' ;

varEntry : 'var' '(' pathName ',' value ')' | 'var' '(' pathName ',' opSignature ')'
			| 'var' '(' pathName (',' varFidelity)+ ')' | 'var' '(' pathName ',' morphFidelity ')'
			| 'var' '(' pathName ',' srvRoutine ')' | 'var' '('(pathName',')? srvEntry')'
			| 'var' '(' pathName ',' varProxy ')' | objectiveVar | constraintVar ;

fiEntry : 'ent' '(' pathName',' entFidelity* ')' ;

entFidelity : 'eFi' '(' contextEntry* ')' ;

contextEntry : dataEntry | procEntry | srvEntry | varEntry | fiEntry
			| entType '(' contextEntry ')' ;

entType : 'in' | 'out' | 'inout' | 'db' ;

sigFidelity : 'sFi' '(' fiName ',' opSignature+ ')' ;

morphFidelity : 'mFi' '(' fiName (',' srvRequest)+ ')'
			| 'mFi' '(' fiName ',' srvMorpher (',' srvRequest)+ ')' ;
			
srvMorpher: morpherLambdaExpression ;

varFidelity : 'vFi' '(' fiName ',' value ')' | 'vFi' '(' fiName ',' opSignature ')'
			| 'vFi' '(' fiName ( ',' srvRoutine)? ( ',' entGetter)? ( ',' entSetter)? ')' ;

varProxy : 'proxy' '(' pathName ',' opSignature ')' ;
		
srvInvoker : 'invoker' '(' javaExpression ',' 'srvArgs' ( ',' dataContext)? ')'
			| 'invoker' '(' opSignature ')' | srvExertion | 'inc' '(' srvInvoker ',' double | int ')'
			| 'methodInvoker()'
			| 'invoker' '('(name',')? valueCallableLambdaExpression ( ',' contextModel)?',' srvArgs ')'
			| procEntry  | conditionalInvoker ;
		
conditionalInvoker :  'loop' '(' srvCondition ',' srvInvoker ')' 
			| 'loop' '(' min ',' max ',' (srvCondition',')? srvInvoker ')' | 'alt' '(' invokeOption* | invokeOption  ')' ;
			
invokeOption : 'opt' '(' srvCondition ',' srvInvoker ')' ;

srvCondition : 'condition' '(' conditionCallableLambda ')'
		    |  'condition' '(' conditionExpression ','  parameterName* ')' ;

srvArgs : 'args' '(' argName+ ')' ;

dependentVars : 'vars' '(' dependentVarName* ')' ;
srvEvaluator : lambdaEvaluator | entEvaluator | objectImplementingEvaluation ;
entGetter : objectImplementingGetter ;
entSetter : objectImplementingSetter ;

/* <MOGRAMS> */

contextModelType : 'entModel' | 'parModel' | 'srvModel' | 'model' | 'mdl' | 'varModel' ;
srvExertionType : 'task' | 'block' | 'job' | conditionalExertion | 'exertion' | 'xrt' ;
conditionalExertionType : 'loop' | 'alt' | 'opt' ;
srvMogramType : contextModel | srvExertion | 'mogram' | 'mog' ;

srvMogram : dataContext  | contextModel | srvExertion | multiFiMogram | 'mogram' '(' contextModelParameters | srvExertionParamters ')' ;

multiFiMogram : 'multiFiReq' '(' (name',')? morphFidelity ')' | 'multiFiReq' '(' (name',')? srvFidelity ')' ;

/* <MODELS> */
srvModel : dataContext | contextModel | varOrientedModel ;
dataContext : 'context ' '(' (name',')? dataEntry* (',' srvResult)? (',' inputPaths)? (',' outputPaths)? ')' 
			| 'tag' '(' dataContext',' annotatedPath ')' | 'tagContext' '(' dataContext',' newTagAssociation ')' ;

contextModel : 'contextModelType' '('(name',' )? contextEntry* (',' 'response' '('pathName*')' (',' srvDependency)? )? ')';
		
parTypes : 'types' '('srvType*')' ;
parArgs : 'args' '('object*')' ;
srvDependency : 'dependsOn' '(' 'ent' '('pathName',' 'paths' '('pathName*')'* ')' ;

/* <TASKS> */
srvTask : 'task' '('(name',')? (opSignature* | sigFidelity* | morphFidelity)',' dataContext')' ;


/* <EXERTIONS> */

srvExertion : srvTask | compoundExertion | 'exertion' '(' srvExertionParamters ')' ;

compoundExertion : srvJob | srvBlock | conditionalExertion ;

srvJob : 'job' '('(name',')? (opSignature | sigFidelity) ',' dataContext ',' srvMogram* ','
				contextPipe* (',' exertionStrategy) (',' dependency)? ',' exertionFidelity* ')' ;

srvBlock :	 'block' '('(name',')? (opSignature | sigFidelity)',' (dataContext',')? 
			srvMogram*',' exertionFidelity* ')' ;

conditionalExertion : 'loop' '('srvCondition',' srvMogram')' 
			| 'loop' '('min',' max',' (srvCondition',')? srvMogram')' | 'alt' '('srvOption*')' ;

srvOption : 'opt' '('srvCondition',' srvMogram')' ;

contextPipe : 'pipe' '(' 'outPoint' '('srvCondition',' contextPathName')' ',' 
			'inPoint' '('srvExertion',' contextPathName')' ')' ;

exertionStrategy : 'strategy' '(' (accessType',')? (flowType',')? (monitorable',')? (provisionable)? ')' ;

flowType: 'Flow.PAR' | 'Flow.SEQ' ;

monitorable : 'Monitor.YES' | 'Monitor.NO' ;

srvFidelity : 'srvFi' '('(fiName',')? srvRequest+')' ; 

fiSelector : 'fi' '('pathName',' fiName')' ;

metaFiSelector : 'fi' '('fiName',' fiSelector+ | compFiSelector+')' ;

multiFi : entFidelity | sigFi | morphFi | varFidelity | srvFidelity ;


/* <ACCESSING-VALUES> */

contextValue : 'value' '('dataContext',' pathName | outputPaths')' 
			| 'valueAt' '('dataContext',' index')' 
			| 'valueAt' '('dataContext',' pathTag')' | 'valuesAt' '('dataContext',' pathTag')' ;

srvValue : 'exec' '(' srvRequest(',' srvArg)*')' | 
			'eval' '('contextEntry(',' srvArg)*')' | 'eval' '('srvModel',' pathName(',' srvArg)*')' 
			| 'eval' '('srvExertion(',' srvArg)*')' | 'returnValue' '('srvMogram')' ; 

srvMogramResult : 'exert' '('srvMogram',' srvArg*')' ;

dataContextResult : 'response' '('srvModel',' srvArg*')' 
			| 'result' '('srvModel (',' pathName)?')' | 'context' '('srvMogram')' 
			| 'upcontext' '('compoundExertion')' ;

srvEntryResult : srvValue | 'get' '('srvMogram',' componentName')' ;

srvArg : opSignature | dataEntry | srvMogram | fiSelector
			| compFiSelector | cxtSelector | inputPaths | outputPaths | srvResult | instanceofArg ;

/* <VAR-ORIENTED-MODELING> */

varOrientedModel : responseModeling | parametricModeling | optimizationModeling	| streamingParametricModeling ;

responseModeling : 'responseModel' '('(modelName',' )? 
					(modelingInstance',' )?  baseVars*',' varRealization*')' ;

parametricModeling : 'paramericModel' '('(modelName',' )? 
					(modelingInstance',' )?  inVars',' outVars',' varRealizations ','mdlTable ')' ;
					
inVars 	: 'inputVars' '(' ((baseVars',')+)? baseVars')' ;
outVars : 'outputVars' '(' ((baseVars',')+)? baseVars')' ;
varRealizations : ((varRealization',')* (varRealization))? ;
mdlTable : 'table' '('varParametricTable',' varResponseTable')' ;

streamingParametricModeling : 'streamingParametricModel' '('(modelName',')?	modelingInstance')' ;

optimizationModeling : 'optimizationModel' '(' (modelName',' )? baseVars*',' varRealization*','
					'objectiveVars' '('objectiveVar+',' 'constraintVars' '('constraintVar+ ')' ')' ;

modelingInstance: 'instance' '('opSignature')' ; 

varType : 'inputVars' | 'outputVars' | 'linkedVars' | 'constantVars' ;

baseVars : 'varType' '('(varName',')+ varName ')' | 'varType' '('varName',' count')' 
					| 'varType' '('varName',' from',' to')' ;

varParametricTable : 'parametricTable' '('tableURL (',' tableSeparator)?')'  
					| 'table' '(' 'header' '('varName+')' ',' 'row' '('value+')'+')' ;

varResponseTable : 'responseTable' '('tableURL (',' tableSeparator)? ')' ;
 
objectiveVar : 'var' '('varName',' outputVarName',' optiTarget ')' ;

optiTarget : 'Target.min' | 'Target.max' ;

constraintVar : 'var' '('varName',' outputVarName',' 'Relation'.relationSuffix ')' ;
 	
relationSuffix: 'lt' | 'lte' | 'eq' | 'gt' | 'gte' ;

varRealization : 'realization' '('varName',' 'fi' '('fiName',' varComponent+')'* ',' 
						'fi' '('fiName',' 'differentiation' '(' 'wrt' '('varName+')' ')'* ')' ;

varComponent : 'evaluator' '('evaluatorName')' | 'getter' '('getterName')' | 'setter' '('setterName')' ;

/* <VAR-ORIENTED-MODELING-TASKS> */

modelingTask : mdlResponseTask | mdlParamericTask | mdlOptimizationTask ;

mdlResponseTask : 'responseTask' '(' 'outerSig' '('selector',' mdlSig ')'(',' responseContext ')')? ')' ;
		
mdlParamericTask : 'parametricTask' '(' 'outerSig' '(' selector',' mdlSig')' ',' paramContext ')' ;

mdlOptimizationTask : 'optimizationTask' '('explorerSignature',' optiContext ',' optiStrategy ')' ;
			
responseContext : 'modelingContext' '(' (mdlInputs',')? (mdlResponses',')? returnPath? ')';

paramContext : 'modelingContext' '(' varParametricTable',' (varResponseTable',')? (mdlParmeters',')?  (mdlResponses',')?  parStrategy? (',' returnPath)?')'; 

optiContext : 'modelingContext' '(' mdlInputs (',' returnPath)?')'; 
	
mdlInputs : 'inputs' '('((dataEntry ',')* dataEntry)')'; 
	 
mdlResponses : 'responses' '('((varName ',')* varName)')';

returnPath : 'result' '('pathName')';

outTable : 'table' '(' varParametricTable(',' varResponseTable)?')';
	
parStrategy : 'parallel' '(' 'queue' '('int')'',' 'pool' '('int')' ')';

mdlParmeters : 'parameters' '('((varName ',')* varName)')'; 
	 						
optiStrategy : 'strategy' '(' optiTarget',' dispatchSig',' mdlSig',' optiSig ')';

dispatchSig : 'dispatcherSig' '('prvSignature')';

mdlSig : 'modelSig' '('prvSignature')';

optiSig : 'optimizerSig' '('prvSignature')';

explorerSignature : opSignature;

/*<END>*/


/* Concrete labels */
argName   : name;
classpath : name;
componentName : name;
configName    : name;
fiName        : name ;
dependency    : name ;
dependentVarName : name ;
parameterName    : name ;
groupName 	     : name ;
getterName 	     : name;
setterName 	     : name;
tableSeparator   : name;
tableURL : url;
url      : 'file://'name | 'http://'name | 'https://'name | 'artifact:'name | 'sos:'name ;

double : intNumber ;
min : int;
max : int;
index : int ;
count : int;
from : int;
to : int;
int : intNumber ;
intNumber : ('0'|'1'|'2'|'3'|'4'|'5'|'6'|'7'|'8'|'9') ;

jarName : name;
locatorName :	 name ;
fromPathName : name ;
toPathName : name ;
pathName :	 name ;
pathTag :	 name ;
newTagAssociation :	 name;
lambdaEvaluator :	 name;
contextPathName	:	 name ;
morphFi :	 name ;
sigFi :	 name ;
entEvaluator :	 name;
objectImplementingEvaluation :	 name;

evaluatorName :	 name;

compFiSelector : name;
exertionFidelity : name ;

contextModelParameters : name;
srvExertionParamters : name;

conditionCallableLambda : name;
conditionExpression : name;
javaExpression	: name;

objectImplementingInvocation : name;
objectImplementingGetter :	name;
objectImplementingSetter :	name;

entrycallableLambdaExpression : name;
serviceLambdaExpression       : name;
callableLambdaExpression      : name;
clientLambdaExpression        : name;
valueCallableLambdaExpression : name;
morpherLambdaExpression       : name;

providerClassName : name ;
providerName	  : name ;	
sigName	  	  : name ;	
selector          : name ;
classSelector     : name ;
serviceName       : name ;
modelName 	  : name ;
varName 	  : name ;
outputVarName	  : name ;
srvTypeName       : name ;	 
value 	          : name ;	
object 		: name ;
instanceofArg   : name ;
class 		: name ;
interface 	: name;
classType 	: name'.class' ;
interfaceType 	: name'.class' ;
name            : ID ;
string_literal       : '"'ID'"' ;

ID                   : ('a'..'z' | 'A'..'Z' | '_') ('a'..'z' | 'A'..'Z' | '0'..'9' | '_')* ;
