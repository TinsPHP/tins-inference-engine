/*
 * This file is part of the TinsPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TINS/License
 */

package ch.tsphp.tinsphp.inference_engine.constraints;

import ch.tsphp.common.symbols.ITypeSymbol;
import ch.tsphp.tinsphp.common.inference.constraints.IBindingCollection;
import ch.tsphp.tinsphp.common.inference.constraints.IFunctionType;
import ch.tsphp.tinsphp.common.inference.constraints.IVariable;
import ch.tsphp.tinsphp.common.symbols.IContainerTypeSymbol;
import ch.tsphp.tinsphp.common.symbols.IConvertibleTypeSymbol;
import ch.tsphp.tinsphp.common.symbols.IIntersectionTypeSymbol;
import ch.tsphp.tinsphp.common.symbols.IParametricTypeSymbol;
import ch.tsphp.tinsphp.common.symbols.IUnionTypeSymbol;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public class TempFunctionType implements IFunctionType
{
    private final IFunctionType functionType;
    private final IBindingCollection bindingCollection;
    private boolean hasConvertibleParameterTypes;


    public TempFunctionType(IFunctionType theFunctionType) {
        functionType = theFunctionType;
        bindingCollection = functionType.getBindingCollection();
        searchConvertibleTypeInTypeBounds(functionType.getParameters());
    }

    private void searchConvertibleTypeInTypeBounds(List<IVariable> parameters) {
        for (IVariable parameter : parameters) {
            String typeVariable = bindingCollection.getTypeVariable(parameter.getAbsoluteName());
            if (searchForConvertibleTypeInTypeBounds(typeVariable)) {
                break;
            }
        }
    }

    private boolean searchForConvertibleTypeInTypeBounds(String typeParameter) {
        if (bindingCollection.hasUpperTypeBounds(typeParameter)) {
            IIntersectionTypeSymbol upperTypeBounds = bindingCollection.getUpperTypeBounds(typeParameter);
            hasConvertibleParameterTypes = containsConvertibleType(upperTypeBounds);
            if (hasConvertibleParameterTypes) {
                return true;
            }
        }

        if (bindingCollection.hasLowerTypeBounds(typeParameter)) {
            IUnionTypeSymbol lowerTypeBounds = bindingCollection.getLowerTypeBounds(typeParameter);
            hasConvertibleParameterTypes = containsConvertibleType(lowerTypeBounds);
            if (hasConvertibleParameterTypes) {
                return true;
            }
        }
        return false;
    }

    private boolean containsConvertibleType(IContainerTypeSymbol typeSymbol) {
        boolean convertibleTypeFound = false;
        for (ITypeSymbol innerTypeSymbol : typeSymbol.getTypeSymbols().values()) {
            if (innerTypeSymbol instanceof IConvertibleTypeSymbol) {
                convertibleTypeFound = true;
                break;
            } else if (innerTypeSymbol instanceof IContainerTypeSymbol) {
                convertibleTypeFound = containsConvertibleType((IContainerTypeSymbol) innerTypeSymbol);
                if (convertibleTypeFound) {
                    break;
                }
            }
        }
        return convertibleTypeFound;
    }

    @Override
    public boolean hasConvertibleParameterTypes() {
        return hasConvertibleParameterTypes;
    }

    @Override
    public String getName() {
        return functionType.getName();
    }

    @Override
    public IBindingCollection getBindingCollection() {
        return bindingCollection;
    }

    @Override
    public int getNumberOfNonOptionalParameters() {
        return functionType.getNumberOfNonOptionalParameters();
    }

    @Override
    public List<IVariable> getParameters() {
        return functionType.getParameters();
    }

    @Override
    public boolean wasSimplified() {
        return false;
    }

    @Override
    public int getNumberOfConvertibleApplications() {
        throw new UnsupportedOperationException("You are dealing with a temp function");
    }

    @Override
    public void manuallySimplified(Set<String> nonFixedTypeParameters, int numberOfConvertibleApplications,
            boolean hasConvertibleParameterTypes) {
        throw new UnsupportedOperationException("You are dealing with a temp function");
    }

    @Override
    public void simplify() {
        throw new UnsupportedOperationException("You are dealing with a temp function");
    }

    @Override
    public String getSignature() {
        throw new UnsupportedOperationException("You are dealing with a temp function");
    }

    @Override
    public String getSuffix(String translatorId) {
        throw new UnsupportedOperationException("You are dealing with a temp function");
    }

    @Override
    public void addSuffix(String translatorId, String newName) {
        throw new UnsupportedOperationException("You are dealing with a temp function");
    }

    @Override
    public boolean wasBound() {
        throw new UnsupportedOperationException("You are dealing with a temp function");
    }

    @Override
    public void fix(String fixedTypeParameter) {
        throw new UnsupportedOperationException("You are dealing with a temp function");
    }

    @Override
    public void renameTypeParameter(String typeParameter, String newTypeParameter) {
        throw new UnsupportedOperationException("You are dealing with a temp function");
    }

    @Override
    public void bindTo(IBindingCollection newBindingCollection, List<String> bindingTypeParameters) {
        throw new UnsupportedOperationException("You are dealing with a temp function");
    }

    @Override
    public void rebind(IBindingCollection newBindingCollection) {
        throw new UnsupportedOperationException("You are dealing with a temp function");
    }

    @Override
    public List<String> getTypeParameters() {
        throw new UnsupportedOperationException("You are dealing with a temp function");
    }

    @Override
    public Set<String> getNonFixedTypeParameters() {
        throw new UnsupportedOperationException("You are dealing with a temp function");
    }

    @Override
    public IFunctionType copy(Collection<IParametricTypeSymbol> parametricTypeSymbols) {
        throw new UnsupportedOperationException("You are dealing with a temp function");
    }

    @Override
    public boolean isFixed() {
        throw new UnsupportedOperationException("You are dealing with a temp function");
    }
}
