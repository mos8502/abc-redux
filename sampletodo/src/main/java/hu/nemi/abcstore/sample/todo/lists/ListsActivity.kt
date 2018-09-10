package hu.nemi.abcstore.sample.todo.lists

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.recyclerview.extensions.ListAdapter
import android.support.v7.util.DiffUtil
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.view.*
import hu.nemi.abcstore.sample.todo.R
import hu.nemi.abcstore.sample.todo.todos.LIST_ID
import hu.nemi.abcstore.sample.todo.todos.TodosActivity

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.android.synthetic.main.item_view_list.view.*
import javax.inject.Inject

class ListsActivity : AppCompatActivity() {

    private val adapter = ListsAdapter()
    @Inject lateinit var viewModel: ListsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ListsComponent(this).inject(this)

        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        lists.layoutManager = LinearLayoutManager(this)
        lists.adapter = adapter
        lists.addOnChildAttachStateChangeListener(object: RecyclerView.OnChildAttachStateChangeListener {
            override fun onChildViewDetachedFromWindow(p0: View) = Unit

            override fun onChildViewAttachedToWindow(view: View) {
                view.setOnClickListener {
                    val position = lists.findContainingViewHolder(it)?.adapterPosition
                    if(position != null && position != RecyclerView.NO_POSITION) {
                        val item = adapter.getItem(position)
                        startActivity(Intent(this@ListsActivity, TodosActivity::class.java).putExtra(LIST_ID, item.id))
                    }
                }
            }
        })

        ItemTouchHelper(object: ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            override fun onMove(p0: RecyclerView, p1: RecyclerView.ViewHolder, p2: RecyclerView.ViewHolder): Boolean = false

            override fun onSwiped(holder: RecyclerView.ViewHolder, direction: Int) {
                viewModel.removeItem(adapter.getItem(holder.adapterPosition).id)
            }
        }).attachToRecyclerView(lists)

        fab.setOnClickListener { view ->
            viewModel.addItem("New List: ${System.currentTimeMillis()}")
        }

        viewModel.lists.observe(this, Observer {
            adapter.submitList(requireNotNull(it))
        })

    }

    @Inject
    fun setViewModelFactory(viewModeFactory: ViewModelProvider.Factory) {
        viewModel = ViewModelProviders.of(this, viewModeFactory).get(ListsViewModel::class.java)
    }

}

private object ListItemCallback: DiffUtil.ItemCallback<ListItem>() {
    override fun areItemsTheSame(oldItem: ListItem, newItem: ListItem): Boolean =
            oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: ListItem, newItem: ListItem): Boolean =
            oldItem == newItem
}

private class ListsAdapter: ListAdapter<ListItem, ListItemViewHolder>(ListItemCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListItemViewHolder =
            ListItemViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_view_list, parent, false))

    override fun onBindViewHolder(holder: ListItemViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    public override fun getItem(position: Int): ListItem {
        return super.getItem(position)
    }
}

private class ListItemViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
    private val title = itemView.title
    private val count = itemView.count

    fun bind(listItem: ListItem) {
        title.text = listItem.title
        count.text = listItem.count.toString()
    }
}

