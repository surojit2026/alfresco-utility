/*
 * Copyright 2016 Acosix GmbH
 *
 * Licensed under the Eclipse Public License (EPL), Version 1.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package de.acosix.alfresco.utility.share.surf;

import java.io.IOException;
import java.util.List;

import org.alfresco.error.AlfrescoRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.extensions.surf.LessCssThemeHandler;

import com.github.sommeri.less4j.Less4jException;
import com.github.sommeri.less4j.LessCompiler.CompilationResult;
import com.github.sommeri.less4j.LessCompiler.Problem;
import com.github.sommeri.less4j.LessCompiler.Problem.Type;
import com.github.sommeri.less4j.core.ThreadUnsafeLessCompiler;

/**
 * @author Axel Faust, <a href="http://acosix.de">Acosix GmbH</a>
 */
public class HybridLess4jCssThemeHandler extends LessCssThemeHandler
{

    // some Share versions do not ship with slf4j-log4j bridges
    private static final Logger LOGGER = LoggerFactory.getLogger(HybridLess4jCssThemeHandler.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public String processCssThemes(final String path, final StringBuilder cssContents) throws IOException
    {
        final String intialProcessResults = super.processCssThemes(path, cssContents);
        final String fullCSS = this.getLessVariables() + intialProcessResults;

        // there appears little point in re-using the compiler instance
        final ThreadUnsafeLessCompiler compiler = new ThreadUnsafeLessCompiler();
        try
        {
            final CompilationResult compilationResult = compiler.compile(fullCSS);
            final String compiledCss = compilationResult.getCss();

            final List<Problem> warnings = compilationResult.getWarnings();

            for (final Problem warning : warnings)
            {
                if (warning.getType() == Type.WARNING)
                {
                    LOGGER.debug("Less4j warning for {} line {} pos {}: {}", path, warning.getLine(), warning.getCharacter(),
                            warning.getMessage());
                }
                else
                {
                    LOGGER.info("Less4j error for {} line {} pos {}: {}", path, warning.getLine(), warning.getCharacter(),
                            warning.getMessage());
                }
            }

            return compiledCss;
        }
        catch (final Less4jException e)
        {
            // default theme handlers fail silently which is annoying as heck
            LOGGER.error("Error processing CSS via Less4j", e);
            throw new AlfrescoRuntimeException("Failed to process CSS via Less4j", e);
        }
    }
}
