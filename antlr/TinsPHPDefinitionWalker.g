/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */
 
/*
 * This file was created based on TSPHPDefinitionWalker.g - the tree grammar file for the definition phase of
 * TSPHP's type checker - and reuses TSPHP's AST class as well as other classes/files related to the AST generation.
 * TSPHP is also licenced under the Apache License 2.0
 * For more information see http://tsphp.ch/wiki/display/TSPHP/License
 */

tree grammar TinsPHPDefinitionWalker;
options {
    tokenVocab = TinsPHP;
    ASTLabelType = ITSPHPAst;
    filter = true;        
}

@header{
/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.antlr;

import ch.tsphp.common.IScope;
import ch.tsphp.common.ITSPHPAst;
import ch.tsphp.tinsphp.common.scopes.INamespaceScope;
import ch.tsphp.tinsphp.common.inference.IDefinitionPhaseController;

}

@members {

private IDefinitionPhaseController definer;
private IScope currentScope;


public TinsPHPDefinitionWalker(TreeNodeStream input, IDefinitionPhaseController theDefiner) {
    this(input);
    definer = theDefiner;    
}

}

topdown
        //scoped symbols
    :   namespaceDefinition   
    |   useDefinitionList
    //TODO rstoll TINS-161 inference OOP
    //|   interfaceDefinition
    //|   classDefinition
    //|   constructDefinition
    
    |   methodFunctionDefinition
    |   blockConditional
    |   foreachLoop
    |   catchBlock
    
        //symbols
    |   constantDefinitionList
    |   variableDeclarationList
    |   parameterDeclarationList
    |   returnBreakContinue
    ;

bottomup
    :   exitNamespace
    |   exitScope
    |   expression
    ;

exitNamespace
    :   Namespace
        {currentScope = currentScope.getEnclosingScope().getEnclosingScope();}
    ;   

exitScope
    :   (   //TODO rstoll TINS-161 inference OOP
            // 'interface'
        //|   'class'
        //|   '__construct'
        //|   METHOD_DECLARATION        
            Function
        |   BLOCK_CONDITIONAL
        |   Foreach
        |   Catch
        ) 
        {
            //only get enclosing scope if a scope was defined - might not be the case due to syntax errors
            if(!(currentScope instanceof INamespaceScope)){
                currentScope = currentScope.getEnclosingScope();
            }
        }
    ;
    
namespaceDefinition
    :   ^(Namespace t=(TYPE_NAME|DEFAULT_NAMESPACE) .)
        {
            currentScope = definer.defineNamespace($t.text);
            $Namespace.setScope(currentScope);
        }
    ;

useDefinitionList
    :   ^('use' useDeclaration+)
    ;
    
useDeclaration
    :   ^(USE_DECLARATION type=TYPE_NAME alias=Identifier)
        {definer.defineUse((INamespaceScope) currentScope, $type, $alias);}
    ;

//TODO rstoll TINS-161 inference OOP    
/*interfaceDefinition
    :   ^('interface' iMod=. identifier=Identifier extIds=. .)
        {currentScope = definer.defineInterface(currentScope, $iMod, $identifier, $extIds); }
    ;
    
classDefinition
    :   ^('class' cMod=. identifier=Identifier extId=. implIds=. .)
        {currentScope = definer.defineClass(currentScope, $cMod, $identifier, $extId, $implIds); }    
    ;
    
constructDefinition
    :   ^(identifier='__construct' mMod=.  ^(TYPE rtMod=. returnType=.) . .)
        {currentScope = definer.defineConstruct(currentScope, $mMod, $rtMod, $returnType, $identifier);}
    ;
*/

methodFunctionDefinition
    :   ^(  //TODO rstoll TINS-161 inference OOP  
            //(   METHOD_DECLARATION
               def=Function
            //)
            mMod=. ^(TYPE rtMod=. returnType=.) Identifier . .
        )
        {
          currentScope = definer.defineMethod(currentScope,$mMod, $rtMod, $returnType, $Identifier); 
          $def.setScope(currentScope);
        }
    ;

blockConditional
    :   ^(block=BLOCK_CONDITIONAL .*)
        {
            currentScope = definer.defineConditionalScope(currentScope);
            $block.setScope(currentScope);
        }    
    ;

foreachLoop
    :   ^(Foreach .*)
        {
            currentScope = definer.defineConditionalScope(currentScope);
            $Foreach.setScope(currentScope);
        }    
    ;

catchBlock
    :   ^(Catch type=. VariableId .*)
         {
             currentScope = definer.defineConditionalScope(currentScope);
             $Catch.setScope(currentScope);
             $type.setScope(currentScope);
         }
    ;

constantDefinitionList
    :   ^(CONSTANT_DECLARATION_LIST ^(TYPE tMod=. type=.) constantDeclaration[$tMod, $type]+)
    ;

constantDeclaration[ITSPHPAst tMod, ITSPHPAst type]
    :   ^(identifier=Identifier .)
        { definer.defineConstant(currentScope,$tMod, $type, $identifier); }
    ;

parameterDeclarationList
    :   ^(PARAMETER_LIST parameterDeclaration+)
    ;

parameterDeclaration
    :   ^(PARAMETER_DECLARATION
            ^(TYPE tMod=. type=.) variableDeclaration[$tMod, $type]
        )
    ;

variableDeclarationList 
    :   ^(VARIABLE_DECLARATION_LIST
            ^(TYPE tMod=. type=.)
                variableDeclaration[$tMod,$type]+
            )
    ;
        
variableDeclaration[ITSPHPAst tMod, ITSPHPAst type]
    :   (   ^(variableId=VariableId .)
        |   variableId=VariableId
        )
        {definer.defineVariable(currentScope, $tMod, $type, $variableId);}
    ;

expression
@after{
    int tokenType = $start.getParent().getType();
    boolean isLogicOperator = tokenType == LogicOrWeak || tokenType == LogicAndWeak 
        || tokenType == LogicOr || tokenType == LogicAnd;
    
    boolean isTernary = tokenType == QuestionMark;       
        
    if(isLogicOperator){
        if($start.getChildIndex() == 0){
        //create conditional scope after first element of a logic operator (due to short circuit)
            currentScope = definer.defineConditionalScope(currentScope);
        } else {
            //reset scope after logic operator
            currentScope = currentScope.getEnclosingScope();
        }
    } else if(isTernary){
        int childIndex = $start.getChildIndex();
        if(childIndex == 0){
            //create conditional scope for if part of ternary
            currentScope = definer.defineConditionalScope(currentScope);
        } else if(childIndex == 1){
            currentScope = currentScope.getEnclosingScope();
            //create conditional scope for else part of ternary
            currentScope = definer.defineConditionalScope(currentScope);
        } else {
            //reset scope after ternary
            currentScope = currentScope.getEnclosingScope();
        }
    }
}
    :   (   identifier=CONSTANT
        |   identifier=VariableId
        //TODO rstoll TINS-161 inference OOP  
        //|   identifier='$this'
        //|   identifier='parent'
        //|   identifier='self'
            //self and parent are already covered above
        //|   ^(CLASS_STATIC_ACCESS identifier=(TYPE_NAME|'self'|'parent') .)
        
        |   ^(CAST ^(TYPE . type=primitiveTypesWithoutResource) .) 
            {
                $identifier=$type.start;
                $start.setScope(currentScope);
            }
        |   ^('instanceof' . (identifier=VariableId | identifier=TYPE_NAME))
            {$start.setScope(currentScope);}
        //TODO rstoll TINS-161 inference OOP          
        //|   ^('new' identifier=TYPE_NAME .)
       //TODO rstoll TINS-161 inference OOP
        //    ^(METHOD_CALL callee=. identifier=Identifier .)
        //    {$callee.setScope(currentScope);}
        //|   ^(METHOD_CALL_STATIC callee=. identifier=Identifier .)
        //    {$callee.setScope(currentScope);}
        //|   ^(METHOD_CALL_POSTFIX identifier=Identifier .)

        |   ^(FUNCTION_CALL identifier=TYPE_NAME .)
        )
        {$identifier.setScope(currentScope);}

        //first child of logic operators or ternary can also be a literal
    |   (   Null
        |   False
        |   True
        |   Int
        |   Float
        |   String
        //expression can be used for key and value in arrays and an assignment could also happen in such an expression
        |   TypeArray

        |   'or'
        |   'xor'
        |   'and'
        
        |   '='
        |   '+='
        |   '-='
        |   '*='
        |   '/='
        |   '&='
        |   '|='
        |   '^='
        |   '%='
        |   '.='
        |   '<<='
        |   '>>='
        
        |   '||'
        |   '&&'
        |   '|'
        |   '^'
        |   '&'
        
        |   '=='
        |   '!='
        |   '==='
        |   '!=='
        
        |   '<'
        |   '<='
        |   '>'
        |   '>='
        
        |   '<<'
        |   '>>'
        
        |   '+'
        |   '-'
        |   '.'
        
        |   '*'
        |   '/'
        |   '%'
        
        |   PRE_INCREMENT
        |   PRE_DECREMENT
        |   '@'
        |   '~'
        |   '!'
        |   UNARY_MINUS
        |   UNARY_PLUS
        |   POST_INCREMENT
        |   POST_DECREMENT
        
        |   ARRAY_ACCESS
        |   'exit'
        )
        {$start.setScope(currentScope);}
        
    |   '?'
        {
            // do not rewrite scope of return type of a function or another unknown type
            if($start.getParent().getType() != TYPE){
                $start.setScope(currentScope);
            }
        }
    ;

primitiveTypesWithoutResource
    :   'bool'
    |   'int'
    |   'float'
    |   'string'
    |   'array'
    ;

returnBreakContinue
    :   (   Return
        |   Break
        |   Continue
        )
        {$start.setScope(currentScope);}
    ;
