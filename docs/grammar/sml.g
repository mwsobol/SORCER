grammar sml;

/* <PROVIDER-RULES> */
srvSignature : prvSignature | opSignature | 'sig' '(' srvSignature ',' signatureOp ')' ;
prvSignature : 'sig' '(' ( name ',' )? prvSpec ')' ;
opSignature  : 'sig' '(' ( name ',' )? opSpec ',' prvSpec ')' ;

prvSpec : (javaType | prvInstance | 'type' '(' srvTypeName ')' (',' 'types' '(' javaType* ')' )? (',' (srvTag | prvTag))? (',' prvDeployment)? ) ;
opSpec  : selector signatureOp ( ',' dataContext )? (',' srvResult)? (',' 'inConn' '(' mapEntry*')')? (',' 'outConn' '(' mapEntry* ')' )? ;

srvType options { backtrack = true; } : classType | interfaceType ;

signatureOp : ('op' '(' selector (',' srvArg*)? ')' | 'op' '(' opSignature ')' ) ;
access : 'Access.PUSH' | 'Access.PULL' ;
provisionable : 'Provision.YES' | 'Provision.NO' ;
prvTag : 'prvName' '(' providerName ')' ;
srvTag : 'srvName' '(' serviceName (',' 'locators'(locatorName*))? (',' groupName*)? ')' ;
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

entType : 'in' | 'out' | 'inout' | 'db' ;
annotatedPath : 'path' '(' pathName ( ',' pathTag)? ')' ;
srvPath : pathName | annotatedPath ;

dataEntry : 'val' '(' srvPath ',' value ')' | 'entTypeVal' '(' srvPath ',' value ')' ;

mapEntry : 'ent' '(' fromPathName ',' toPathName ')' ;

procEntry : ('ent' '(' opSignature ')' | 'ent' '(' pathName ',' srvEvaluator ')' | 'ent' '(' pathName ',' srvInvoker ( ',' srvModel)? ')' | lambdaEntry  ')') ;

lambdaEntry : 'lambda' '(' pathName ',' entrycallableLambdaExpression ')'
			| 'lambda' '(' pathName ',' serviceLambdaExpression',' srvArgs ')'
			| 'lambda' '(' pathName ',' callableLambdaExpression',' srvArgs ')'
			| 'lambda' '(' pathName ',' clientLambdaExpression',' srvArgs ')'
			| 'lambda' '(' pathName ',' valueCallableLambdaExpression',' srvArgs ')' ;

srvEntry options { backtrack = true; } : 'ent' '(' pathName ',' opSignature ( ',' srvModel)? ( ',' cxtSelector)? ')' 
			| 'ent' '(' pathName ',' srvRoutine ')' | 'ent' '(' pathName ',' srvMogram ')' ;

cxtSelector : selector '(' (componentName',' )? pathName+ ')' ;

srvRoutine : contextEntry | srvInvoker | srvEvaluator ;

varEntry : 'var' '(' pathName ',' value ')' | 'var' '(' pathName ',' opSignature ')'
			| 'var' '(' pathName ',' varFidelity+ ')' | 'var' '(' pathName ',' morphFidelity* ')'
			| 'var' '(' pathName ',' lambdaEvaluator ')' | 'var' '(' pathName ',' varProxy ')' ;

fiEntry : 'ent' '(' pathName',' entFidelity* ')' ;

entFidelity : 'eFi' '(' contextEntry* ')' ;

contextEntry : dataEntry | srvEntry | varEntry | fiEntry
			| entType '(' contextEntry ')' ;

sigFidelity : 'sFi' '(' fiName ',' opSignature+ ')' ;

morphFidelity : 'mFi' '(' fiName ',' srvRequest+ ')'
			| 'mFi' '(' fiName ',' srvMorpher ',' srvRequest+ ')' ;
			
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
		
parTypes : 'types' '('class*')' ;
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

exertionStrategy : 'strategy' '(' (access',')? (flowType',')? (monitorable',')? (provisionable)? ')' ;

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

srvValue : 'exec' '(' srvRequest',' arg*')' | 
			'eval' '('contextEntry',' srvArg*')' | 'eval' '('srvModel',' pathName',' srvArg *')' 
			| 'eval' '('srvExertion',' srvArg*')' | 'returnValue' '('srvMogram')' ; 

srvMogramResult : 'exert' '('srvMogram',' srvArg*')' ;

dataContextResult : 'response' '('srvModel',' srvArg*')' 
			| 'result' '('srvModel (',' pathName)?')' | 'context' '('srvMogram')' 
			| 'upcontext' '('compoundExertion')' ;

srvEntryResult : srvValue | 'get' '('srvMogram',' componentName')' ;

srvArg : instanceofArg'.class' | opSignature | dataEntry | srvMogram | fiSelector
			| compFiSelector | cxtSelector | inputPaths | outputPaths | srvResult ;

/* <VAR-ORIENTED-MODELING> */

varOrientedModel : responseModeling | parametricModeling | optimizationModeling	| streamingParametricModeling ;

responseModeling : 'responseModel' '('(modelName',' )? 
					(modelingInstance',' )?  baseVar*',' varRealization*')' ;

parametricModeling : 'paramericModel' '('(modelName',' )? 
					(modelingInstance',' )?  baseVar*',' varRealization*','
					'table' '('varParametricTable',' varResponseTable')' ')' ;

streamingParametricModeling : 'streamingParametricModel' '('(modelName',')?	modelingInstance')' ;

optimizationModeling : 'optimizationModel' '(' (modelName',' )? baseVar*',' varRealization*','
					'objectiveVars' '('objectiveVar+',' 'constraintVars' '('constraintVar+ ')' ')' ;

modelingInstance: 'instance' '('opSignature')' ; 

varType : 'input' | 'output' | 'linked' | 'constant' ;

baseVar : 'varTypeVars' '('varName+')' | 'varTypeVars' '('varName',' count')' 
					| 'varTypeVars' '('varName',' from',' to')' | 'var' '('(name',')? srvEntry')' ;

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

mdlResponseTask: 'responseTask' '(' 'outerSig' '('selector',' modelSig ')'
		(',' 'modelingContext' '(' ('inputs' '('dataEntry *')' ',' )? ('responses' '('varName*')' ',' )? 
			(',' 'result' '('pathName')' )? ')')? ')' ;

mdlParamericTask : 'parametricTask' '(' 'outerSig' '(' selector',' modelSig')' ','
		'modelingContext' '(' varParametricTable',' varResponseTable','	(',' 'parameters' '('varName*')')? (',' 'responses' '('varName*')')?
		(',' 'parallel' '(' 'queue' '('int',' 'pool' '('int')' ')' ')' )? (',' 'result' '('pathName')')? ')' ')' ;

mdlOptimizationTask: 'optimizationTask' '('explorerSignature',' 'exploreContext' '(' 'initialDesgn' '('dataEntry *')' 
                        (',' 'result' '('pathName')')? ')' ','
		                'strategy' '('optiTarget',' 'dispatcherSig' '('prvSignature')' ',' 
						'modelSig' '('opSignature')' ',' 'optimizerSig' '('prvSignature')' ')' ')' ;

modelSig : opSignature ;

explorerSignature : opSignature;

/*<END>*/


/* Concrete labels */
arg       : name;
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

selector          : name ;
serviceName       : name ;
modelName 	      : name ;
varName 	      : name ;
outputVarName	  : name ;
srvTypeName       : name ;	 
value 	          : name ;	

instanceofArg   : javaType ;
class           : javaType ;
object          : name ;
interfaceType 	: classType ;
classType 	    : javaType;
javaType        : class_or_package'.class' ;
class_or_package     : ID ;
name                 : ID ;
string_literal       : '"'ID'"' ;

ID                   : ('a'..'z' | 'A'..'Z' | '_') ('a'..'z' | 'A'..'Z' | '0'..'9' | '_')* ;
