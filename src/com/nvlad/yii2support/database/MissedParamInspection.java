package com.nvlad.yii2support.database;

import com.google.common.collect.Lists;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiErrorElement;
import com.jetbrains.php.lang.inspections.PhpInspection;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.visitors.PhpElementVisitor;
import com.nvlad.yii2support.common.ClassUtils;
import com.nvlad.yii2support.objectfactory.ObjectFactoryUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by oleg on 30.03.2017.
 */
public class MissedParamInspection extends PhpInspection {
    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder problemsHolder, boolean isOnTheFly) {
        return new PhpElementVisitor() {

            @Override
            public void visitPhpMethodReference(MethodReference reference) {
                if (reference != null) {
                    Method method = (Method) reference.resolve();
                    if (method == null)
                        return;
                    if ( method.getParameters().length > 1 &&
                            method.getParameters()[1].getName().equals("params") &&
                            method.getParameters()[0].getName().equals("condition")) {
                        if (reference.getParameters().length == 1 || (reference.getParameters().length > 1 && reference.getParameters()[1] instanceof PsiErrorElement)) {
                            PsiElement element = reference.getParameters()[0];
                            if (element instanceof StringLiteralExpression) {
                                String condition = ((StringLiteralExpression) element).getContents();
                                String[] conditionParams = DatabaseUtils.extractParamsFromCondition(condition);
                                if (conditionParams.length > 0) {
                                    MissedParamQuickFix qFix = new MissedParamQuickFix(reference);
                                    problemsHolder.registerProblem(reference.getParameters()[0], "Condition parameters must be defined", qFix);
                                }
                            }
                        }
                    }
                }
                super.visitPhpMethodReference(reference);
            }

            @Override
            public void visitPhpArrayCreationExpression(ArrayCreationExpression expression) {
                MethodReference methodRef = ClassUtils.getMethodRef(expression, 10);
                if (methodRef != null) {
                    Method method = (Method) methodRef.resolve();
                    if (method == null)
                        return;
                    int paramPosition = ClassUtils.paramIndexForElement(expression);
                    if (paramPosition > 0 && method.getParameters().length > paramPosition) {
                        if (method.getParameters()[paramPosition].getName().equals("params") &&
                                method.getParameters()[paramPosition - 1].getName().equals("condition")) {
                            PsiElement element = methodRef.getParameters()[paramPosition - 1];
                            if (element instanceof StringLiteralExpression) {
                                String condition = ((StringLiteralExpression) element).getContents();
                                String[] conditionParams = DatabaseUtils.extractParamsFromCondition(condition);
                                List<ArrayHashElement> hashElements = Lists.newArrayList(expression.getHashElements());
                                String[] params = new String[hashElements.size()];
                                for (int i = 0; i < hashElements.size(); i++ ){
                                    PsiElement key = hashElements.get(i).getKey();
                                    if (key != null)
                                        params[i] = ClassUtils.removeQuotes(key.getText());
                                }

                                if (! Arrays.equals(conditionParams, params) ) {
                                    MissedParamQuickFix qFix = new MissedParamQuickFix(methodRef);
                                    String problemDesc = "Parameters do not correspond to the condition";
                                    if (params.length == 1)
                                        problemDesc = "Parameter does not correspond to the condition";
                                    problemsHolder.registerProblem(expression, problemDesc, qFix);
                                }
                            }
                        }
                    }
                }

                super.visitPhpArrayCreationExpression(expression);
            }
        };
    }
}