package se.eelde.toggles

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import se.eelde.toggles.applications.applicationNavigations
import se.eelde.toggles.composetheme.TogglesTheme
import se.eelde.toggles.configurationlist.configurationsNavigations
import se.eelde.toggles.dialogs.booleanvalue.BooleanValueView
import se.eelde.toggles.dialogs.enumvalue.EnumValueView
import se.eelde.toggles.dialogs.integervalue.IntegerValueView
import se.eelde.toggles.dialogs.stringvalue.StringValueView
import se.eelde.toggles.help.HelpView
import se.eelde.toggles.oss.detail.OssDetailView
import se.eelde.toggles.oss.list.OssListView

@Suppress("LongMethod")
@Composable
fun Navigation(navController: NavHostController, paddingValues: PaddingValues) {
    NavHost(
        modifier = Modifier.padding(paddingValues),
        navController = navController,
        startDestination = "applications"
    ) {
        applicationNavigations(navigateToConfigurations = { applicationId ->
            navController.navigate("configurations/$applicationId")
        })
        configurationsNavigations(navController)
        composable(
            "configuration/{configurationId}/{scopeId}/boolean",
            arguments = listOf(
                navArgument("configurationId") { type = NavType.LongType },
                navArgument("scopeId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            BooleanValueView(
                navController
            )
        }
        composable(
            "configuration/{configurationId}/{scopeId}/integer",
            arguments = listOf(
                navArgument("configurationId") { type = NavType.LongType },
                navArgument("scopeId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            IntegerValueView(
                navController
            )
        }
        composable(
            "configuration/{configurationId}/{scopeId}/string",
            arguments = listOf(
                navArgument("configurationId") { type = NavType.LongType },
                navArgument("scopeId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            StringValueView(
                navController
            )
        }
        composable(
            "configuration/{configurationId}/{scopeId}/enum",
            arguments = listOf(
                navArgument("configurationId") { type = NavType.LongType },
                navArgument("scopeId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            EnumValueView(
                navController
            )
        }
        composable(
            "oss",
        ) {
            OssListView(navController = navController)
        }
        composable(
            "oss/{dependency}/{skip}/{length}",
            arguments = listOf(
                navArgument("dependency") { type = NavType.StringType },
                navArgument("skip") { type = NavType.IntType },
                navArgument("length") { type = NavType.IntType },
            )
        ) {
            OssDetailView()
        }
        composable(
            "help",
        ) {
            HelpView()
        }
    }
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TogglesTheme {
                val navController: NavHostController = rememberNavController()

                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = colorScheme.background
                ) {
                    ModalNavigationDrawer(
                        drawerState = drawerState,
                        drawerContent = { DrawerLaLa(navController, drawerState) }
                    ) {
                        Scaffold(
                            topBar = {
                                TogglesAppBar(navController, drawerState)
                            },
                        ) {
                            Navigation(navController, it)
                        }
                    }
                }
            }
        }
    }

    @ExperimentalMaterial3Api
    @Composable
    @Suppress("LongMethod")
    fun DrawerLaLa(navController: NavHostController, drawerState: DrawerState) {
        // https://developer.android.com/reference/kotlin/androidx/compose/material3/package-summary#ModalNavigationDrawer(kotlin.Function0,androidx.compose.ui.Modifier,androidx.compose.material3.DrawerState,kotlin.Boolean,androidx.compose.ui.graphics.Color,kotlin.Function0)
        val scope = rememberCoroutineScope()
        val selectedItem = remember { mutableStateOf(0) }
        ModalDrawerSheet {
            Spacer(Modifier.height(12.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    modifier = Modifier.background(colorResource(id = R.color.toggles_blue)),
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = "Application icon"
                )
            }
            Spacer(Modifier.height(12.dp))
            NavigationDrawerItem(
                icon = {
                    Icon(
                        painterResource(id = R.drawable.ic_settings_white_24dp),
                        contentDescription = null
                    )
                },
                label = { Text(stringResource(id = R.string.applications)) },
                selected = 0 == selectedItem.value,
                onClick = {
                    scope.launch { drawerState.close() }
                    navController.navigate("applications")
                    selectedItem.value = 0
                },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
            NavigationDrawerItem(
                icon = { Icon(painterResource(id = R.drawable.ic_oss), contentDescription = null) },
                label = { Text(stringResource(id = R.string.oss)) },
                selected = 1 == selectedItem.value,
                onClick = {
                    scope.launch { drawerState.close() }
                    navController.navigate("oss")
                    selectedItem.value = 1
                },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
            NavigationDrawerItem(
                icon = {
                    Icon(
                        painterResource(id = R.drawable.ic_report_black_24dp),
                        contentDescription = null
                    )
                },
                label = { Text(stringResource(id = R.string.help)) },
                selected = 2 == selectedItem.value,
                onClick = {
                    scope.launch { drawerState.close() }
                    navController.navigate("help")
                    selectedItem.value = 2
                },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
        }
    }

    @Composable
    @OptIn(ExperimentalMaterial3Api::class)
    fun TogglesAppBar(navController: NavHostController, drawerState: DrawerState) {
        val currentRoute = navController.currentBackStackEntryAsState()

        val rootId = currentRoute.value?.destination?.parent?.startDestinationRoute
        val currentId = currentRoute.value?.destination?.route
        val rootItem = rootId == currentId

        val scope = rememberCoroutineScope()
        TogglesAppBar(root = rootItem) {
            if (rootItem) {
                scope.launch { drawerState.open() }
            } else {
                navController.navigateUp()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun TogglesAppBar(root: Boolean, navigationIconClicked: () -> Unit) {
        TopAppBar(
            title = { Text(stringResource(id = R.string.app_name)) },
            navigationIcon = if (root) {
                {
                    IconButton(onClick = { navigationIconClicked() }) {
                        Icon(imageVector = Icons.Filled.Menu, contentDescription = null)
                    }
                }
            } else {
                {

                    IconButton(onClick = { navigationIconClicked() }) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = null)
                    }
                }
            }
        )
    }
}