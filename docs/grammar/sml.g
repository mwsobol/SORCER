grammar sml;

/*
 * Signatures
 * A provider signature is a service provider reference (handle) specified by a service type. The role of provider  
 * signatures declaring provider services is similar to constructors in object-oriented programming. An operation signature  
 * expending a provider signature is an executable provider service - exec(signature). An operation signature can be customized 
 * with the following options: signature name, signature operation name (selector), provider name, implemented types, groups, 
 * locators, data context, return result format, input and output connectors.
 */
srvSignature : prvSignature | opSignature | 'sig' '(' srvSignature ',' signatureOp ')' ;
prvSignature : 'sig' '(' ( name ',' )? prvSpec ')' ;
opSignature  : 'sig' '(' ( name ',' )? opSpec ',' prvSpec ')' ;

prvSpec : (javaType | prvInstance | 'type' '(' srvTypeName ')' (',' 'types' '(' javaType* ')' )? (',' (srvTag | prvTag))? (',' prvDeployment)? ) ;
opSpec  : selector signatureOp ( ',' dataContext )? (',' srvResult)? (',' 'inConn' '(' mapEntry*')')? (',' 'outConn' '(' mapEntry* ')' )? ;
srvType : classType | interfaceType ;
signatureOp : ('op' '(' selector (',' srvArg*)? ')' | 'op' '(' opSignature ')' ) ;
access : 'Access.PUSH' | 'Access.PULL' ;
provisionable : 'Provision.YES' | 'Provision.NO' ;
prvTag : 'prvName' '(' providerName ')' ;
srvTag : 'srvName' '(' serviceName (',' 'locators'(locatorName*))? (',' groupName*)? ')' ;
srvResult : 'result' '(' pathName? (',' inputPaths)? (',' outputPaths)? (',' dataContext)? ')' ;
prvDeployment : 'deploy' '(' 'implementation' '(' providerClassName ')' 
               'classpath' '(' jarName* ')' 'codebase' '(' jarName* 'configuration' '('configName ')' 'maintain' '(' intNumber ')' 'idle' '(' intNumber ')' ')' ;
inputPaths : 'inPaths' '(' srvPath+ ')' ;
outputPaths : 'outPaths' '(' srvPath+ ')' ;

/*
Provider Services
A service provider is an instance of local or remote concrete service specified by a 
signature. Service mograms are bound to service providers at runtime by the SORCER platform. 
 */
prvInstance : 'prv' '(' srvSignature ')' ;
 		
/*
 * Requests
 * Elementary service requests are called items and compound requests are called mograms. 
 * For example, signatures, context entries, and service fidelities are items. Context models 
 * and exertions are mograms.
 */
srvRequest : srvSignature | contextEntry | srvMogram ;

/*
Entries
An entry is a functional association of a path and a function body of an underlying context model. 
A path is a function name as a sequence of attributes that define modeling namespace. 
A body of an entry specifies a return value of the entry. A body defining a function composition 
depends on paths of other entries in the model scope.
*/
entType : 'in' | 'out' | 'inout' | 'db' ;
annotatedPath : 'path' '(' pathName ( ',' pathTag)? ')' ;
srvPath : pathName | annotatedPath ;

dataEntry : 'val' '(' srvPath ',' value ')' | 'entTypeVal' '(' srvPath ',' value ')' ;

mapEntry : 'ent' '(' fromPathName ',' toPathName ')' ;

procEntry : 'ent' '(' opSignature) | 'ent' '(' pathName ',' srvEvaluator ')' 
			| 'ent' '(' pathName ',' srvInvoker ( ',' srvModel)?) | procEntry | lambdaEntry  ')' ;

lambdaEntry : 'lambda' '(' pathName ',' EntryCallableLambdaExpression ')'
			| 'lambda' '(' pathName ',' ServiceLambdaExpression, srvArgs ')'
			| 'lambda' '(' pathName ',' CallableLambdaExpression, srvArgs ')'
			| 'lambda' '(' pathName ',' ClientLambdaExpression)?, srvArgs ')'
			| 'lambda' '(' pathName ',' ValueCallableLambdaExpression, srvArgs ')' ;

srvEntry : 'ent' '(' pathName ',' opSignature ( ',' srvModel)? ( ',' cxtSelector)? ')' 
			| 'ent' '(' pathName ',' srvRoutine ')' | 'ent' '(' pathName ',' srvMogram ')' ;

cxtSelector : selector '(' [componentName, )? pathName+ ')' ;

srvRoutine : contextEntry | srvInvoker | srvEvaluator ;

varEntry : 'var' '(' pathName ',' value ')' | 'var' '(' pathName ',' opSignature ')'
			| 'var' '(' pathName ',' varFidelity+ ')' | 'var' '(' pathName ',' morphFidelity* ')'
			| 'var' '(' pathName ',' lambdaEvaluator ')' | 'var' '(' pathName ',' varProxy ')' ;

fiEntry : 'ent' '(' pathName, entFidelity* ')' ;

entFidelity : 'eFi' '(' contextEntry* ')' ;

contextEntry : dataEntry | srvEntry | varEntry | fiEntry
			| entType '(' contextEntry ')' ;

sigFidelity : 'sFi' '(' fiName ',' opSignature+ ')' ;

morphFidelity : 'mFi' '(' fiName ',' srvRequest+ ')'
			| 'mFi' '(' fiName ',' srvMorpher ',' srvRequest+ ')' ;
			
srvMorpher: MorpherLambdaExpression ;

varFidelity : 'vFi' '(' fiName ',' value ')' | 'vFi' '(' fiName ',' opSignature ')'
			| 'vFi' '(' fiName ( ',' srvRoutine)? ( ',' entGetter)? ( ',' entSetter)? ')' ;

varProxy : 'proxy' '(' pathName ',' opSignature ')' ;
		
srvInvoker : 'invoker' '(' JavaExpression ',' 'srvArgs' ( ',' dataContext)? ')'
			| 'invoker' '(' opSignature ')' | srvExertion | 'inc' '(' srvInvoker ',' double | int ')'
			| 'methodInvoker()'
			| 'invoker' '('(name',')? ValueCallableLambdaExpression ( ',' contextModel)?, srvArgs ')'
			| procEntry  | conditionalInvoker ;
		
conditionalInvoker :  'loop' '(' srvCondition ',' srvInvoker ')' ;
			| 'loop' '(' min ',' max ',' (srvCondition',')? srvInvoker ')' | 'alt' '(' invokeOption* | invokeOption  ')' ;
			
invokeOption : 'opt' '(' srvCondition ',' srvInvoker ')' ;

srvCondition : 'condition' '(' ConditionCallableLambda ')'
		    |  'condition' '(' conditionExpression ','  parameterName* ')' ;

srvArgs : 'args' '(' argName+ ')' ;

dependentVars : 'vars' '(' dependentVarName* ')' ;
srvEvaluator : lambdaEvaluator | entEvaluator | objectImplementingEvaluation ;
srvInvoker : objectImplementingInvocation ;
entGetter : objectImplementingGetter ;
entSetter : objectImplementingSetter ;

/*
 * Mograms
 * Mograms are compound requests that specify service federations. A context model is a declarative 
 * specification and an exertion is a procedural one for a dynamically bound federation of 
 * collaborating service providers.
*/
contextModelType : 'entModel' | 'parModel' | 'srvModel' | 'model' | 'mdl' | 'varModel' ;
srvExertionType : 'task' | 'block' | 'job' | conditionalExertion | 'exertion' | 'xrt' ;
conditionalExertionType : 'loop' | 'alt' | 'opt' ;
srvMogramType : contextModel | srvExertion | 'mogram' | 'mog' ;

/*		
model == mdl
context == cxt
exertion == xrt 
mogram == mog
*/
srvMogram : dataContext  | contextModel | srvExertion | multiFiMogram | 'mogram' '(' contextModelParameters | srvExertionParamters ')' ;

multiFiMogram : 'multiFiReq' '(' (name,)? morphFidelity ')' | 'multiFiReq' '(' (name,)? srvFidelity)

/*
 * Models
 * A model is an aggregation of entries representing service federations as functionals. 
 * A data context is composed of entries of the dataEntry type and a context model of 
 * entries of the contextEntry type.
 */
srvModel : dataContext | contextModel | varOrientedModel ;
dataContext : 'context ' '(' (name,)? dataEntry* (, srvResult)? (, inputPaths)? (, outputPaths)? ')' 
			| 'tag' '(' dataContext, annotatedPath ')' | 'tagContext' '(' dataContext, newTagAssociation ')' ;
		contextModel	 : 'contextModelType' '('(name, )? contextEntry* (, 'response' '('pathName*) (, srvDependency)?')')? ')';
parTypes : 'types' '('class*')' ;
parArgs : 'args' '('object*')' ;
srvDependency : 'dependsOn' '(' 'ent' '('pathName, 'paths' '('pathName*')'* ')' ;

/*
 * Tasks
 * A task specifies an action of provider service or concatenation (batch) of provider 
 services processing data context.
 */
srvTask : 'task' '('(name,)? [opSignature* | sigFidelity* | sigMorphFidelity], [dataContext]')' ;


/* Concrete labels*/
argName : name;
classpath : name;
componentName :	 name;
configName :	 name;
fiName : name ;
dependentVarName :	 name ;
parameterName :	 name ;
groupName 	:	 name ;
double : intNumber ;
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

entEvaluator 	:	 name;
objectImplementingEvaluation 
	:	 name;

contextModelParameters :	 name;
srvExertionParamters :	 name;

ConditionCallableLambda :	 name;
conditionExpression :	 name;
JavaExpression	:	 name;

objectImplementingInvocation :	 name;
objectImplementingGetter :	 name;
objectImplementingSetter :	 name;

EntryCallableLambdaExpression :	 name;
ServiceLambdaExpression :	 name;
CallableLambdaExpression :	 name;
ClientLambdaExpression :	 name;
ValueCallableLambdaExpression :	 name;
MorpherLambdaExpression :	 name;

providerClassName :	 name;
providerName	: name ;	

selector : name;
serviceName     :	name ;
srvArg : name;
//srvPath :	 name;
srvTypeName          : name ;	 
value 	:	 name ;	

class :	 javaType ;
object : name ;
interfaceType 	: classType ;
classType 	    : javaType;
javaType        : class_or_package'.class' ;
class_or_package     : ID;
name                 : string_literal ;
string_literal       : '"'ID'"';
ID                   : ('a'..'z'|'A'..'Z'|'_') ('a'..'z'|'A'..'Z'|'0'..'9'|'_')*;
