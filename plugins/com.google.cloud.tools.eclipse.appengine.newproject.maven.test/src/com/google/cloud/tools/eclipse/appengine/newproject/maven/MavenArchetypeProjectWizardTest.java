package com.google.cloud.tools.eclipse.appengine.newproject.maven;

import static com.google.cloud.tools.eclipse.appengine.newproject.maven.MavenAppEngineStandardWizardPage.suggestPackageName;

import static org.junit.Assert.assertNotNull;

import org.eclipse.swt.widgets.Display;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class MavenArchetypeProjectWizardTest {

  private MavenArchetypeProjectWizard wizard;

  @Before
  public void setUp() {
    assertNotNull(Display.getDefault());

    wizard = new MavenArchetypeProjectWizard();
    wizard.addPages();
  }
  
  @Test
  public void testCanFinish() {
    Assert.assertFalse(wizard.canFinish());
  }

  @Test
  public void testTwoPages() {
    Assert.assertEquals(2, wizard.getPageCount());
  }
  
  @Test
  public void testGetPageByName() {
    assertNotNull(wizard.getPage("basicNewProjectPage"));
    assertNotNull(wizard.getPage("newProjectArchetypePage"));
  }

  @Test
  public void testPageComplete() {
    Assert.assertFalse(wizard.getPage("newProjectArchetypePage").isPageComplete());
    Assert.assertFalse(wizard.getPage("newProjectArchetypePage").isPageComplete());
  }

  @Test
  public void testFlipToPageTwo() {
    Assert.assertFalse(wizard.getPage("basicNewProjectPage").canFlipToNextPage());
  }

  @Test
  public void testPageOrder() {
    Assert.assertEquals(wizard.getPage("newProjectArchetypePage"),
        wizard.getPage("basicNewProjectPage").getNextPage());
    Assert.assertEquals(wizard.getPage("basicNewProjectPage"),
        wizard.getPage("newProjectArchetypePage").getPreviousPage());
  }

  @Test
  public void testArchetypeDefaultSelection() {
    Assert.assertEquals("appengine-skeleton-archetype",
        MavenAppEngineStandardArchetypeWizardPage.PRESET_ARCHETYPES.get(0)
            .archetype.getArtifactId());
  }

  @Test
  public void testSuggestPackageName() {
    Assert.assertEquals("aa.bb", suggestPackageName("aa.bb", ""));
    Assert.assertEquals("aa.bb.cc.dd", suggestPackageName("aa.bb", "cc.dd"));

    Assert.assertEquals("aa.bb", suggestPackageName("aA.Bb", ""));
    Assert.assertEquals("aa.bb.cc.dd", suggestPackageName("Aa.Bb", "Cc.DD"));

    Assert.assertEquals("aa.bb", suggestPackageName(" a  a\t . b\r b \n", " \t \r  "));
    Assert.assertEquals("aa.bb.cc.dd", suggestPackageName("  A  a.\tBb", " C  c . D D  "));

    Assert.assertEquals("aa.bb", suggestPackageName("....aa....bb...", "......"));
    Assert.assertEquals("aa.bb.cc.dd", suggestPackageName("....aa....bb...", "..cc....dd.."));

    Assert.assertEquals("aa._01234bb",
        suggestPackageName("aa`~!@#$%^&*()-+=[]{}<>\\|:;'\",?/._01234bb", ""));
    Assert.assertEquals("aa._01234bb._c_c_0.dd01234_",
        suggestPackageName("aa`~!@#$%^&*()-+=[]{}<>\\|:;'\",?/._01234bb",
            "_c_c_0.dd01234_`~!@#$%^&*()-+=[]{}<>\\|:;'\",?/"));
  }
}
