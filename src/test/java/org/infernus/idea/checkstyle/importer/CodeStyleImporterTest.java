package org.infernus.idea.checkstyle.importer;

import com.intellij.lang.java.JavaLanguage;
import com.intellij.lang.xml.XMLLanguage;
import com.intellij.openapi.project.Project;
import com.intellij.psi.codeStyle.*;
import com.intellij.psi.codeStyle.arrangement.match.ArrangementMatchRule;
import com.intellij.psi.codeStyle.arrangement.match.ArrangementSectionRule;
import com.intellij.psi.codeStyle.arrangement.match.StdArrangementEntryMatcher;
import com.intellij.psi.codeStyle.arrangement.match.StdArrangementMatchRule;
import com.intellij.psi.codeStyle.arrangement.model.ArrangementAtomMatchCondition;
import com.intellij.psi.codeStyle.arrangement.model.ArrangementCompositeMatchCondition;
import com.intellij.psi.codeStyle.arrangement.model.ArrangementMatchCondition;
import com.intellij.psi.codeStyle.arrangement.std.StdArrangementExtendableSettings;
import com.intellij.psi.codeStyle.arrangement.std.StdArrangementRuleAliasToken;
import com.intellij.psi.codeStyle.arrangement.std.StdArrangementSettings;
import com.intellij.testFramework.LightPlatformTestCase;
import java.util.Collections;
import java.util.List;
import org.infernus.idea.checkstyle.CheckstyleProjectService;
import org.infernus.idea.checkstyle.config.PluginConfiguration;
import org.infernus.idea.checkstyle.config.PluginConfigurationBuilder;
import org.infernus.idea.checkstyle.config.PluginConfigurationManager;
import org.infernus.idea.checkstyle.csapi.CheckstyleInternalObject;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;

import static com.intellij.psi.codeStyle.arrangement.std.StdArrangementTokens.EntryType.CONSTRUCTOR;
import static com.intellij.psi.codeStyle.arrangement.std.StdArrangementTokens.EntryType.FIELD;
import static com.intellij.psi.codeStyle.arrangement.std.StdArrangementTokens.EntryType.METHOD;
import static com.intellij.psi.codeStyle.arrangement.std.StdArrangementTokens.Modifier.PACKAGE_PRIVATE;
import static com.intellij.psi.codeStyle.arrangement.std.StdArrangementTokens.Modifier.PRIVATE;
import static com.intellij.psi.codeStyle.arrangement.std.StdArrangementTokens.Modifier.PROTECTED;
import static com.intellij.psi.codeStyle.arrangement.std.StdArrangementTokens.Modifier.PUBLIC;
import static com.intellij.psi.codeStyle.arrangement.std.StdArrangementTokens.Modifier.STATIC;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CodeStyleImporterTest
        extends LightPlatformTestCase {
    private CodeStyleSettings codeStyleSettings;
    private CommonCodeStyleSettings javaSettings;

    private final Project project = mock(Project.class);
    private CheckstyleProjectService csService = null;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        PluginConfigurationManager mockPluginConfig = mock(PluginConfigurationManager.class);
        final PluginConfiguration mockConfigDto = PluginConfigurationBuilder.testInstance("8.0").build();
        when(mockPluginConfig.getCurrent()).thenReturn(mockConfigDto);
        when(project.getService(PluginConfigurationManager.class)).thenReturn(mockPluginConfig);

        csService = new CheckstyleProjectService(project);

        codeStyleSettings = CodeStyleSettingsManager.createTestSettings(CodeStyleSettings.getDefaults());
        javaSettings = codeStyleSettings.getCommonSettings(JavaLanguage.INSTANCE);
    }

    private static final String FILE_PREFIX =
            """
                    <?xml version="1.0"?>
                    <!DOCTYPE module PUBLIC
                              "-//Puppy Crawl//DTD Check Configuration 1.3//EN"
                              "http://www.puppycrawl.com/dtds/configuration_1_3.dtd">
                    <module name = "Checker">
                    """;
    private static final String FILE_SUFFIX =
            "</module>";

    private void importConfiguration(@NotNull final String configuration) {
        String fullConfiguration = FILE_PREFIX + configuration + FILE_SUFFIX;

        new CheckStyleCodeStyleImporter(csService).importConfiguration(
                csService, loadConfiguration(fullConfiguration), codeStyleSettings);
    }

    private String inTreeWalker(@NotNull final String configuration) {
        return "<module name=\"TreeWalker\">" + configuration + "</module>";
    }

    private CheckstyleInternalObject loadConfiguration(@NotNull final String configuration) {
        return csService.getCheckstyleInstance().loadConfiguration(configuration);
    }

    public void testImportRightMargin() {
        importConfiguration(
                inTreeWalker(
                        """
                                <module name="LineLength">
                                    <property name="max" value="100"/>
                                </module>"""
                )
        );
        assertEquals(100, javaSettings.RIGHT_MARGIN);
    }

    public void testEmptyLineSeparator() {
        javaSettings.BLANK_LINES_AROUND_FIELD = 0;
        javaSettings.BLANK_LINES_AROUND_METHOD = 0;
        importConfiguration(
                inTreeWalker(
                        """
                                <module name="EmptyLineSeparator">
                                    <property name="tokens" value="VARIABLE_DEF, METHOD_DEF"/>
                                </module>"""
                )
        );
        assertEquals(1, javaSettings.BLANK_LINES_AROUND_FIELD);
        assertEquals(1, javaSettings.BLANK_LINES_AROUND_METHOD);
    }

    public void testImportFileTabCharacter() {
        CommonCodeStyleSettings xmlSettings = codeStyleSettings.getCommonSettings(XMLLanguage.INSTANCE);
        CommonCodeStyleSettings.IndentOptions javaIndentOptions = javaSettings.getIndentOptions();
        assertNotNull(javaIndentOptions);
        CommonCodeStyleSettings.IndentOptions xmlIndentOptions = xmlSettings.getIndentOptions();
        assertNotNull(xmlIndentOptions);
        javaIndentOptions.USE_TAB_CHARACTER = true;
        xmlIndentOptions.USE_TAB_CHARACTER = true;
        importConfiguration(
                inTreeWalker(
                        """
                                <module name="FileTabCharacter">
                                    <property name="eachLine" value="true" />
                                    <property name="fileExtensions" value="java,xml" />
                                </module>"""
                )
        );
        assertFalse(javaIndentOptions.USE_TAB_CHARACTER);
        assertFalse(xmlIndentOptions.USE_TAB_CHARACTER);
    }

    public void testImportFileTabCharacterNoExplicitExtensions() {
        CommonCodeStyleSettings xmlSettings = codeStyleSettings.getCommonSettings(XMLLanguage.INSTANCE);
        CommonCodeStyleSettings.IndentOptions javaIndentOptions = javaSettings.getIndentOptions();
        assertNotNull(javaIndentOptions);
        CommonCodeStyleSettings.IndentOptions xmlIndentOptions = xmlSettings.getIndentOptions();
        assertNotNull(xmlIndentOptions);
        javaIndentOptions.USE_TAB_CHARACTER = true;
        xmlIndentOptions.USE_TAB_CHARACTER = true;
        importConfiguration(
                inTreeWalker(
                        "<module name=\"FileTabCharacter\"/>\n"
                )
        );
        assertFalse(javaIndentOptions.USE_TAB_CHARACTER);
        assertFalse(xmlIndentOptions.USE_TAB_CHARACTER);
    }

    public void testImportWhitespaceAfter() {
        javaSettings.SPACE_AFTER_COMMA = false;
        javaSettings.SPACE_AFTER_SEMICOLON = false;
        javaSettings.SPACE_AFTER_TYPE_CAST = false;
        importConfiguration(
                inTreeWalker(
                        """
                                <module name="WhitespaceAfter">
                                    <property name="tokens" value="COMMA, SEMI"/>
                                </module>"""
                )
        );
        assertTrue(javaSettings.SPACE_AFTER_COMMA);
        assertTrue(javaSettings.SPACE_AFTER_SEMICOLON);
        assertFalse(javaSettings.SPACE_AFTER_TYPE_CAST);
    }

    public void testImportWhitespaceAround() {
        javaSettings.SPACE_AROUND_ASSIGNMENT_OPERATORS = false;
        javaSettings.SPACE_AROUND_EQUALITY_OPERATORS = false;
        javaSettings.SPACE_AROUND_BITWISE_OPERATORS = false;
        importConfiguration(
                inTreeWalker(
                        """
                                <module name="WhitespaceAround">
                                    <property name="tokens" value="ASSIGN"/>
                                    <property name="tokens" value="EQUAL"/>
                                </module>"""
                )
        );
        assertTrue(javaSettings.SPACE_AROUND_ASSIGNMENT_OPERATORS);
        assertTrue(javaSettings.SPACE_AROUND_EQUALITY_OPERATORS);
        assertFalse(javaSettings.SPACE_AROUND_BITWISE_OPERATORS);
    }

    public void testNoWhitespaceBeforeImporter() {
        javaSettings.SPACE_BEFORE_SEMICOLON = true;
        javaSettings.SPACE_BEFORE_COMMA = true;
        importConfiguration(
                inTreeWalker(
                        "<module name=\"NoWhitespaceBefore\"/>"
                )
        );
        assertFalse(javaSettings.SPACE_BEFORE_SEMICOLON);
        assertFalse(javaSettings.SPACE_BEFORE_COMMA);
    }

    public void testLeftCurlyImporter() {
        javaSettings.CLASS_BRACE_STYLE = CommonCodeStyleSettings.NEXT_LINE_SHIFTED;
        javaSettings.METHOD_BRACE_STYLE = CommonCodeStyleSettings.NEXT_LINE_SHIFTED;
        javaSettings.BRACE_STYLE = CommonCodeStyleSettings.NEXT_LINE_SHIFTED;
        importConfiguration(
                inTreeWalker(
                        """
                                <module name="LeftCurly">
                                    <property name="option" value="nl"/>
                                    <property name="tokens" value="CLASS_DEF,INTERFACE_DEF"/>
                                </module>
                                <module name="LeftCurly">
                                    <property name="option" value="eol"/>
                                    <property name="tokens" value="METHOD_DEF,LITERAL_IF"/>
                                </module>"""
                )
        );
        assertEquals(CommonCodeStyleSettings.NEXT_LINE, javaSettings.CLASS_BRACE_STYLE);
        assertEquals(CommonCodeStyleSettings.END_OF_LINE, javaSettings.METHOD_BRACE_STYLE);
        assertEquals(CommonCodeStyleSettings.END_OF_LINE, javaSettings.BRACE_STYLE);
    }

    public void testNeedBracesImporter() {
        javaSettings.DOWHILE_BRACE_FORCE = CommonCodeStyleSettings.DO_NOT_FORCE;
        javaSettings.IF_BRACE_FORCE = CommonCodeStyleSettings.DO_NOT_FORCE;
        javaSettings.FOR_BRACE_FORCE = CommonCodeStyleSettings.DO_NOT_FORCE;
        importConfiguration(
                inTreeWalker(
                        """
                                <module name="NeedBraces">
                                    <property name="allowSingleLineStatement" value="true"/>
                                </module>"""
                )
        );
        assertEquals(CommonCodeStyleSettings.FORCE_BRACES_IF_MULTILINE, javaSettings.DOWHILE_BRACE_FORCE);
        assertEquals(CommonCodeStyleSettings.FORCE_BRACES_IF_MULTILINE, javaSettings.IF_BRACE_FORCE);
        assertEquals(CommonCodeStyleSettings.FORCE_BRACES_IF_MULTILINE, javaSettings.FOR_BRACE_FORCE);
    }

    public void testIndentationImporter() {
        javaSettings.INDENT_BREAK_FROM_CASE = false;
        CommonCodeStyleSettings.IndentOptions indentOptions = javaSettings.getIndentOptions();
        assertNotNull(indentOptions);
        indentOptions.INDENT_SIZE = 8;
        indentOptions.CONTINUATION_INDENT_SIZE = 8;
        importConfiguration(
                inTreeWalker(
                        """
                                 <module name="Indentation">
                                            <property name="basicOffset" value="2"/>
                                            <property name="braceAdjustment" value="0"/>
                                            <property name="caseIndent" value="2"/>
                                            <property name="throwsIndent" value="4"/>
                                            <property name="lineWrappingIndentation" value="4"/>
                                            <property name="arrayInitIndent" value="2"/>
                                </module>"""
                )
        );
        javaSettings.INDENT_BREAK_FROM_CASE = true;
        indentOptions.INDENT_SIZE = 2;
        indentOptions.CONTINUATION_INDENT_SIZE = 4;
    }

    public void testImportOrderImporter() {
        // group attribute
        {
            importConfiguration(
                    inTreeWalker(
                            """
                                     <module name="ImportOrder">
                                                <property name="groups" value="my.custom.package,java,*"/>
                                    </module>"""
                    )
            );
            PackageEntry[] expected = new PackageEntry[]{
                    new PackageEntry(false, "my.custom.package", true),
                    new PackageEntry(false, "java", true),
                    PackageEntry.ALL_OTHER_IMPORTS_ENTRY
            };

            comparePackageEntries(expected, codeStyleSettings.getCustomSettings(JavaCodeStyleSettings.class).IMPORT_LAYOUT_TABLE);
        }

        // staticPosition attribute - top
        {
            importConfiguration(
                    inTreeWalker(
                            """
                                     <module name="ImportOrder">
                                                <property name="groups" value="my.custom.package,*"/>
                                                <property name="option" value="top"/>
                                    </module>"""
                    )
            );
            PackageEntry[] expected = new PackageEntry[]{
                    PackageEntry.ALL_OTHER_STATIC_IMPORTS_ENTRY,
                    new PackageEntry(false, "my.custom.package", true),
                    PackageEntry.ALL_OTHER_IMPORTS_ENTRY,
            };

            comparePackageEntries(expected, codeStyleSettings.getCustomSettings(JavaCodeStyleSettings.class).IMPORT_LAYOUT_TABLE);
        }

        // staticPosition attribute - bottom
        {
            importConfiguration(
                    inTreeWalker(
                            """
                                     <module name="ImportOrder">
                                                <property name="groups" value="my.custom.package,*"/>
                                                <property name="option" value="bottom"/>
                                    </module>"""
                    )
            );
            PackageEntry[] expected = new PackageEntry[]{
                    new PackageEntry(false, "my.custom.package", true),
                    PackageEntry.ALL_OTHER_IMPORTS_ENTRY,
                    PackageEntry.ALL_OTHER_STATIC_IMPORTS_ENTRY,
            };

            comparePackageEntries(expected, codeStyleSettings.getCustomSettings(JavaCodeStyleSettings.class).IMPORT_LAYOUT_TABLE);
        }

        // staticPosition attribute - above
        {
            importConfiguration(
                    inTreeWalker(
                            """
                                     <module name="ImportOrder">
                                                <property name="groups" value="my.custom.package,*"/>
                                                <property name="option" value="above"/>
                                    </module>"""
                    )
            );
            PackageEntry[] expected = new PackageEntry[]{
                    new PackageEntry(true, "my.custom.package", true),
                    new PackageEntry(false, "my.custom.package", true),
                    PackageEntry.ALL_OTHER_STATIC_IMPORTS_ENTRY,
                    PackageEntry.ALL_OTHER_IMPORTS_ENTRY,
            };

            comparePackageEntries(expected, codeStyleSettings.getCustomSettings(JavaCodeStyleSettings.class).IMPORT_LAYOUT_TABLE);
        }

        // staticPosition attribute - under
        {
            importConfiguration(
                    inTreeWalker(
                            """
                                     <module name="ImportOrder">
                                                <property name="groups" value="my.custom.package,*"/>
                                                <property name="option" value="under"/>
                                    </module>"""
                    )
            );
            PackageEntry[] expected = new PackageEntry[]{
                    new PackageEntry(false, "my.custom.package", true),
                    new PackageEntry(true, "my.custom.package", true),
                    PackageEntry.ALL_OTHER_IMPORTS_ENTRY,
                    PackageEntry.ALL_OTHER_STATIC_IMPORTS_ENTRY,
            };

            comparePackageEntries(expected, codeStyleSettings.getCustomSettings(JavaCodeStyleSettings.class).IMPORT_LAYOUT_TABLE);
        }

        // staticPosition attribute - inflow
        {
            importConfiguration(
                    inTreeWalker(
                            """
                                     <module name="ImportOrder">
                                                <property name="groups" value="my.custom.package,*"/>
                                                <property name="option" value="inflow"/>
                                    </module>"""
                    )
            );
            PackageEntry[] expected = new PackageEntry[]{
                    new PackageEntry(false, "my.custom.package", true),
                    PackageEntry.ALL_OTHER_IMPORTS_ENTRY,
            };

            assertFalse(codeStyleSettings.getCustomSettings(JavaCodeStyleSettings.class).LAYOUT_STATIC_IMPORTS_SEPARATELY);
            comparePackageEntries(expected, codeStyleSettings.getCustomSettings(JavaCodeStyleSettings.class).IMPORT_LAYOUT_TABLE);
        }

        // separated attribute - top
        {
            importConfiguration(
                    inTreeWalker(
                            """
                                     <module name="ImportOrder">
                                                <property name="groups" value="my.custom.package,*"/>
                                                <property name="option" value="top"/>
                                                <property name="separated" value="true"/>
                                    </module>"""
                    )
            );
            PackageEntry[] expected = new PackageEntry[]{
                    PackageEntry.ALL_OTHER_STATIC_IMPORTS_ENTRY,
                    PackageEntry.BLANK_LINE_ENTRY,
                    new PackageEntry(false, "my.custom.package", true),
                    PackageEntry.BLANK_LINE_ENTRY,
                    PackageEntry.ALL_OTHER_IMPORTS_ENTRY,
            };

            assertFalse(codeStyleSettings.getCustomSettings(JavaCodeStyleSettings.class).LAYOUT_STATIC_IMPORTS_SEPARATELY);
            comparePackageEntries(expected, codeStyleSettings.getCustomSettings(JavaCodeStyleSettings.class).IMPORT_LAYOUT_TABLE);
        }

        // separate attribute - bottom
        {
            importConfiguration(
                    inTreeWalker(
                            """
                                     <module name="ImportOrder">
                                                <property name="groups" value="my.custom.package,*"/>
                                                <property name="option" value="bottom"/>
                                                <property name="separated" value="true"/>
                                    </module>"""
                    )
            );
            PackageEntry[] expected = new PackageEntry[]{
                    new PackageEntry(false, "my.custom.package", true),
                    PackageEntry.BLANK_LINE_ENTRY,
                    PackageEntry.ALL_OTHER_IMPORTS_ENTRY,
                    PackageEntry.BLANK_LINE_ENTRY,
                    PackageEntry.ALL_OTHER_STATIC_IMPORTS_ENTRY,
            };

            assertFalse(codeStyleSettings.getCustomSettings(JavaCodeStyleSettings.class).LAYOUT_STATIC_IMPORTS_SEPARATELY);
            comparePackageEntries(expected, codeStyleSettings.getCustomSettings(JavaCodeStyleSettings.class).IMPORT_LAYOUT_TABLE);
        }

        // separate attribute - above
        {
            importConfiguration(
                    inTreeWalker(
                            """
                                     <module name="ImportOrder">
                                                <property name="groups" value="my.custom.package,*"/>
                                                <property name="option" value="above"/>
                                                <property name="separated" value="true"/>
                                    </module>"""
                    )
            );
            PackageEntry[] expected = new PackageEntry[]{
                    new PackageEntry(true, "my.custom.package", true),
                    new PackageEntry(false, "my.custom.package", true),
                    PackageEntry.BLANK_LINE_ENTRY,
                    PackageEntry.ALL_OTHER_STATIC_IMPORTS_ENTRY,
                    PackageEntry.ALL_OTHER_IMPORTS_ENTRY,
            };

            comparePackageEntries(expected, codeStyleSettings.getCustomSettings(JavaCodeStyleSettings.class).IMPORT_LAYOUT_TABLE);
        }

        // separate attribute - under
        {
            importConfiguration(
                    inTreeWalker(
                            """
                                     <module name="ImportOrder">
                                                <property name="groups" value="my.custom.package,*"/>
                                                <property name="option" value="under"/>
                                                <property name="separated" value="true"/>
                                    </module>"""
                    )
            );
            PackageEntry[] expected = new PackageEntry[]{
                    new PackageEntry(false, "my.custom.package", true),
                    new PackageEntry(true, "my.custom.package", true),
                    PackageEntry.BLANK_LINE_ENTRY,
                    PackageEntry.ALL_OTHER_IMPORTS_ENTRY,
                    PackageEntry.ALL_OTHER_STATIC_IMPORTS_ENTRY,
            };

            comparePackageEntries(expected, codeStyleSettings.getCustomSettings(JavaCodeStyleSettings.class).IMPORT_LAYOUT_TABLE);
        }

        // separate attribute - inflow
        {
            importConfiguration(
                    inTreeWalker(
                            """
                                     <module name="ImportOrder">
                                                <property name="groups" value="my.custom.package,*"/>
                                                <property name="option" value="inflow"/>
                                                <property name="separated" value="true"/>
                                    </module>"""
                    )
            );
            PackageEntry[] expected = new PackageEntry[]{
                    new PackageEntry(false, "my.custom.package", true),
                    PackageEntry.BLANK_LINE_ENTRY,
                    PackageEntry.ALL_OTHER_IMPORTS_ENTRY,
            };

            assertFalse(codeStyleSettings.getCustomSettings(JavaCodeStyleSettings.class).LAYOUT_STATIC_IMPORTS_SEPARATELY);
            comparePackageEntries(expected, codeStyleSettings.getCustomSettings(JavaCodeStyleSettings.class).IMPORT_LAYOUT_TABLE);
        }
    }

    private static void comparePackageEntries(final PackageEntry[] expected, final PackageEntryTable actual) {
        assertEquals(expected.length, actual.getEntryCount());
        for (int x = 0; x < expected.length; x++) {
            assertEquals(expected[x], actual.getEntries()[x]);
        }
    }

    public void testAvoidStartImportImporter() {
        resetAvoidStarImportSettings(codeStyleSettings);
        importConfiguration(
                inTreeWalker(
                        " <module name=\"AvoidStarImport\">\n"
                                + "</module>"
                )
        );
        JavaCodeStyleSettings customSettings = codeStyleSettings.getCustomSettings(JavaCodeStyleSettings.class);

        assertEquals(999, customSettings.CLASS_COUNT_TO_USE_IMPORT_ON_DEMAND);
        assertEquals(999, customSettings.NAMES_COUNT_TO_USE_IMPORT_ON_DEMAND);
        assertEquals(0, customSettings.PACKAGES_TO_USE_IMPORT_ON_DEMAND.getEntryCount());

        resetAvoidStarImportSettings(codeStyleSettings);
        importConfiguration(
                inTreeWalker(
                        """
                                 <module name="AvoidStarImport">
                                            <property name="allowClassImports" value="true"/>
                                            <property name="allowStaticMemberImports" value="true"/>
                                </module>"""
                )
        );

        assertEquals(1, customSettings.NAMES_COUNT_TO_USE_IMPORT_ON_DEMAND);
        assertEquals(1, customSettings.CLASS_COUNT_TO_USE_IMPORT_ON_DEMAND);
        assertEquals(0, customSettings.PACKAGES_TO_USE_IMPORT_ON_DEMAND.getEntryCount());

        resetAvoidStarImportSettings(codeStyleSettings);
        importConfiguration(
                inTreeWalker(
                        """
                                 <module name="AvoidStarImport">
                                            <property name="allowStaticMemberImports" value="true"/>
                                </module>"""
                )
        );

        assertEquals(999, customSettings.CLASS_COUNT_TO_USE_IMPORT_ON_DEMAND);
        assertEquals(1, customSettings.NAMES_COUNT_TO_USE_IMPORT_ON_DEMAND);
        assertEquals(0, customSettings.PACKAGES_TO_USE_IMPORT_ON_DEMAND.getEntryCount());

        customSettings.CLASS_COUNT_TO_USE_IMPORT_ON_DEMAND = 1;

        resetAvoidStarImportSettings(codeStyleSettings);
        importConfiguration(
                inTreeWalker(
                        """
                                 <module name="AvoidStarImport">
                                            <property name="allowClassImports" value="true"/>
                                </module>"""
                )
        );

        assertEquals(1, customSettings.CLASS_COUNT_TO_USE_IMPORT_ON_DEMAND);
        assertEquals(999, customSettings.NAMES_COUNT_TO_USE_IMPORT_ON_DEMAND);
        assertEquals(0, customSettings.PACKAGES_TO_USE_IMPORT_ON_DEMAND.getEntryCount());

        resetAvoidStarImportSettings(codeStyleSettings);
        importConfiguration(
                inTreeWalker(
                        """
                                 <module name="AvoidStarImport">
                                            <property name="excludes" value="a.b.c,d.e.f"/>
                                </module>"""
                )
        );

        PackageEntry[] expected = new PackageEntry[]{
                new PackageEntry(false, "a.b.c", false),
                new PackageEntry(false, "d.e.f", false),
        };

        comparePackageEntries(expected, customSettings.PACKAGES_TO_USE_IMPORT_ON_DEMAND);
    }

    private static void resetAvoidStarImportSettings(final CodeStyleSettings settings) {
        JavaCodeStyleSettings customSettings = settings.getCustomSettings(JavaCodeStyleSettings.class);
        customSettings.NAMES_COUNT_TO_USE_IMPORT_ON_DEMAND = 1;
        customSettings.CLASS_COUNT_TO_USE_IMPORT_ON_DEMAND = 1;
        customSettings.PACKAGES_TO_USE_IMPORT_ON_DEMAND.copyFrom(new PackageEntryTable());
    }

    public void testDeclarationOrderImporter() {

        // no attributes
        {
            codeStyleSettings.setArrangementSettings(new StdArrangementSettings(Collections.emptyList()));
            importConfiguration(
                inTreeWalker(
                    """
                             <module name="DeclarationOrder">
                            </module>"""
                )
            );
            StdArrangementExtendableSettings actual = (StdArrangementExtendableSettings) codeStyleSettings.getArrangementSettings();
            ArrangementMatchCondition[] expectedSectionsMatchers =
                new ArrangementMatchCondition[] {
                    new ArrangementCompositeMatchCondition(
                        List.of(
                            new ArrangementAtomMatchCondition(FIELD),
                            new ArrangementAtomMatchCondition(STATIC),
                            new ArrangementAtomMatchCondition(PUBLIC))),
                    new ArrangementCompositeMatchCondition(
                        List.of(
                            new ArrangementAtomMatchCondition(FIELD),
                            new ArrangementAtomMatchCondition(STATIC),
                            new ArrangementAtomMatchCondition(PROTECTED))),
                    new ArrangementCompositeMatchCondition(
                        List.of(
                            new ArrangementAtomMatchCondition(FIELD),
                            new ArrangementAtomMatchCondition(STATIC),
                            new ArrangementAtomMatchCondition(PACKAGE_PRIVATE))),
                    new ArrangementCompositeMatchCondition(
                        List.of(
                            new ArrangementAtomMatchCondition(FIELD),
                            new ArrangementAtomMatchCondition(STATIC),
                            new ArrangementAtomMatchCondition(PRIVATE))),
                    new ArrangementCompositeMatchCondition(
                        List.of(
                            new ArrangementAtomMatchCondition(FIELD),
                            new ArrangementAtomMatchCondition(PUBLIC))),
                    new ArrangementCompositeMatchCondition(
                        List.of(
                            new ArrangementAtomMatchCondition(FIELD),
                            new ArrangementAtomMatchCondition(PROTECTED))),
                    new ArrangementCompositeMatchCondition(
                        List.of(
                            new ArrangementAtomMatchCondition(FIELD),
                            new ArrangementAtomMatchCondition(PACKAGE_PRIVATE))),
                    new ArrangementCompositeMatchCondition(
                        List.of(
                            new ArrangementAtomMatchCondition(FIELD),
                            new ArrangementAtomMatchCondition(PRIVATE))),
                    new ArrangementAtomMatchCondition(CONSTRUCTOR),
                    new ArrangementAtomMatchCondition(METHOD)
                };

            assertNotNull(actual);
            assertDeclarationOrderVisibilityAlias(actual.getRuleAliases().toArray(new StdArrangementRuleAliasToken[0]));
            assertEquals(0, actual.getGroupings().size());
            assertDeclarationOrderSectionComments(actual.getSections());
            assertDeclarationOrderSectionOrderType(actual.getSections());
            ArrangementMatchCondition[] actualSectionMatchers = actual.getSections().stream()
                .flatMap(rules -> rules.getMatchRules().stream())
                .map(rule -> rule.getMatcher().getCondition()).toList().toArray(new ArrangementMatchCondition[0]);
            Assert.assertArrayEquals(expectedSectionsMatchers, actualSectionMatchers);
        }

        // ignoreConstructors attribute
        {
            codeStyleSettings.setArrangementSettings(new StdArrangementSettings(Collections.emptyList()));
            importConfiguration(
                inTreeWalker(
                    """
                             <module name="DeclarationOrder">
                               <property name="ignoreConstructors" value="true"/>
                            </module>"""
                )
            );
            StdArrangementExtendableSettings actual = (StdArrangementExtendableSettings) codeStyleSettings.getArrangementSettings();
            ArrangementMatchCondition[] expectedSectionsMatchers =
                new ArrangementMatchCondition[] {
                    new ArrangementCompositeMatchCondition(
                        List.of(
                            new ArrangementAtomMatchCondition(FIELD),
                            new ArrangementAtomMatchCondition(STATIC),
                            new ArrangementAtomMatchCondition(PUBLIC))),
                    new ArrangementCompositeMatchCondition(
                        List.of(
                            new ArrangementAtomMatchCondition(FIELD),
                            new ArrangementAtomMatchCondition(STATIC),
                            new ArrangementAtomMatchCondition(PROTECTED))),
                    new ArrangementCompositeMatchCondition(
                        List.of(
                            new ArrangementAtomMatchCondition(FIELD),
                            new ArrangementAtomMatchCondition(STATIC),
                            new ArrangementAtomMatchCondition(PACKAGE_PRIVATE))),
                    new ArrangementCompositeMatchCondition(
                        List.of(
                            new ArrangementAtomMatchCondition(FIELD),
                            new ArrangementAtomMatchCondition(STATIC),
                            new ArrangementAtomMatchCondition(PRIVATE))),
                    new ArrangementCompositeMatchCondition(
                        List.of(
                            new ArrangementAtomMatchCondition(FIELD),
                            new ArrangementAtomMatchCondition(PUBLIC))),
                    new ArrangementCompositeMatchCondition(
                        List.of(
                            new ArrangementAtomMatchCondition(FIELD),
                            new ArrangementAtomMatchCondition(PROTECTED))),
                    new ArrangementCompositeMatchCondition(
                        List.of(
                            new ArrangementAtomMatchCondition(FIELD),
                            new ArrangementAtomMatchCondition(PACKAGE_PRIVATE))),
                    new ArrangementCompositeMatchCondition(
                        List.of(
                            new ArrangementAtomMatchCondition(FIELD),
                            new ArrangementAtomMatchCondition(PRIVATE))),
                    new ArrangementAtomMatchCondition(METHOD)
                };

            assertNotNull(actual);
            assertDeclarationOrderVisibilityAlias(actual.getRuleAliases().toArray(new StdArrangementRuleAliasToken[0]));
            assertEquals(0, actual.getGroupings().size());
            assertDeclarationOrderSectionComments(actual.getSections());
            assertDeclarationOrderSectionOrderType(actual.getSections());
            ArrangementMatchCondition[] actualSectionMatchers = actual.getSections().stream()
                .flatMap(rules -> rules.getMatchRules().stream())
                .map(rule -> rule.getMatcher().getCondition()).toList().toArray(new ArrangementMatchCondition[0]);
            Assert.assertArrayEquals(expectedSectionsMatchers, actualSectionMatchers);
        }

        // ignoreModifiers attribute
        {
            codeStyleSettings.setArrangementSettings(new StdArrangementSettings(Collections.emptyList()));
            importConfiguration(
                inTreeWalker(
                    """
                             <module name="DeclarationOrder">
                               <property name="ignoreModifiers" value="true"/>
                            </module>"""
                )
            );
            StdArrangementExtendableSettings actual = (StdArrangementExtendableSettings) codeStyleSettings.getArrangementSettings();
            ArrangementMatchCondition[] expectedSectionsMatchers =
                new ArrangementMatchCondition[] {
                    new ArrangementCompositeMatchCondition(
                        List.of(
                            new ArrangementAtomMatchCondition(FIELD),
                            new ArrangementAtomMatchCondition(STATIC))),
                    new ArrangementAtomMatchCondition(FIELD),
                    new ArrangementAtomMatchCondition(CONSTRUCTOR),
                    new ArrangementAtomMatchCondition(METHOD)
                };

            assertNotNull(actual);
            assertDeclarationOrderVisibilityAlias(actual.getRuleAliases().toArray(new StdArrangementRuleAliasToken[0]));
            assertEquals(0, actual.getGroupings().size());
            assertDeclarationOrderSectionComments(actual.getSections());
            assertDeclarationOrderSectionOrderType(actual.getSections());
            ArrangementMatchCondition[] actualSectionMatchers = actual.getSections().stream()
                .flatMap(rules -> rules.getMatchRules().stream())
                .map(rule -> rule.getMatcher().getCondition()).toList().toArray(new ArrangementMatchCondition[0]);
            Assert.assertArrayEquals(expectedSectionsMatchers, actualSectionMatchers);
        }
    }

    private static void assertDeclarationOrderVisibilityAlias(StdArrangementRuleAliasToken[] ruleAliases){
        Assert.assertArrayEquals(
            new StdArrangementRuleAliasToken[] {
                new StdArrangementRuleAliasToken(
                    "visibility",
                    List.of(
                        new StdArrangementMatchRule(new StdArrangementEntryMatcher(new ArrangementAtomMatchCondition(PUBLIC))),
                        new StdArrangementMatchRule(new StdArrangementEntryMatcher(new ArrangementAtomMatchCondition(PROTECTED))),
                        new StdArrangementMatchRule(new StdArrangementEntryMatcher(new ArrangementAtomMatchCondition(PACKAGE_PRIVATE))),
                        new StdArrangementMatchRule(new StdArrangementEntryMatcher(new ArrangementAtomMatchCondition(PRIVATE)))
                    )
                )
            },
            ruleAliases);
    }

    private static void assertDeclarationOrderSectionComments(List<ArrangementSectionRule> sectionRules) {
        sectionRules.forEach(section -> {
            assertNull(section.getEndComment());
            assertNull(section.getStartComment());
        });
    }

    private static void assertDeclarationOrderSectionOrderType(List<ArrangementSectionRule> sectionRules) {
        sectionRules.forEach(section ->
            section.getMatchRules().forEach(matchRule ->
                assertEquals(matchRule.getOrderType(), ArrangementMatchRule.DEFAULT_ORDER_TYPE)));
    }
}
