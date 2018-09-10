package hu.nemi.abcstore.sample.todo.todos

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.recyclerview.extensions.ListAdapter
import android.support.v7.util.DiffUtil
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import hu.nemi.abcstore.sample.todo.R
import kotlinx.android.synthetic.main.activity_todos.*
import kotlinx.android.synthetic.main.content_todos.*
import kotlinx.android.synthetic.main.item_view_todo.view.*
import javax.inject.Inject

class TodosActivity : AppCompatActivity() {

    private val adapter = TodosAdapter()
    @Inject lateinit var viewModel: TodosViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_todos)
        setSupportActionBar(toolbar)

        TodosComponent.inject(this)

        todo.layoutManager = LinearLayoutManager(this)
        todo.adapter = adapter
        ItemTouchHelper(object: ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            override fun onMove(p0: RecyclerView, p1: RecyclerView.ViewHolder, p2: RecyclerView.ViewHolder): Boolean = false

            override fun onSwiped(holder: RecyclerView.ViewHolder, direction: Int) {
                viewModel.removeItem(adapter.getItem(holder.adapterPosition).id)
            }
        }).attachToRecyclerView(todo)

        fab.setOnClickListener { view ->
            viewModel.addItem("TODO: ${System.currentTimeMillis()}")
        }

        viewModel.todos.observe(this, Observer {
            adapter.submitList(requireNotNull(it?.todos))
            title = it?.listTitle
        })
    }

    @Inject
    fun setViewModelFactory(viewModeFactory: ViewModelProvider.Factory) {
        viewModel = ViewModelProviders.of(this, viewModeFactory).get(TodosViewModel::class.java)
    }

}

const val LIST_ID = "list-id"

private object TodoItemCallback : DiffUtil.ItemCallback<Todo>() {
    override fun areItemsTheSame(oldItem: Todo, newItem: Todo): Boolean =
            oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: Todo, newItem: Todo): Boolean =
            oldItem == newItem
}

private class TodosAdapter : ListAdapter<Todo, TodoViewHolder>(TodoItemCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TodoViewHolder =
            TodoViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_view_todo, parent, false))

    override fun onBindViewHolder(holder: TodoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    public override fun getItem(position: Int): Todo {
        return super.getItem(position)
    }
}

private class TodoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val text = itemView.text

    fun bind(todo: Todo) {
        text.text = todo.text
    }
}

